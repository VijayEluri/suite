package suite.funp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTag;
import suite.funp.P0.FunpTagId;
import suite.funp.P0.FunpTagValue;
import suite.funp.P0.FunpTypeCheck;
import suite.funp.P0.FunpVariable;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.persistent.PerMap;
import suite.primitive.IntMutable;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.Read;
import suite.util.List_;
import suite.util.String_;

public class P1Inline {

	private Inspect inspect = Singleton.me.inspect;

	public Funp inline(Funp node, int rounds, int f0, int f1, int f2, int f3, int f4) {
		node = renameVariables(node);

		for (var i = 0; i < rounds; i++) {
			node = 0 < f0 ? inlineDefineAssigns(node) : node;
			node = 0 < f1 ? inlineDefines(node) : node;
			node = 0 < f2 ? inlineFields(node) : node;
			node = 0 < f3 ? inlineLambdas(node) : node;
			node = 0 < f4 ? inlineTags(node) : node;
		}

		return node;
	}

	// Before - v => v => v
	// After - v => v$1 => v$1
	private Funp renameVariables(Funp node) {
		var vns = new HashSet<>();

		Iterate<String> newVarName = vn -> {
			var vn1 = vn.split("\\$")[0];
			var i = 0;
			while (!vns.add(vn1))
				vn1 = vn + "$" + i++;
			return vn1;
		};

		class Rename {
			private PerMap<String, String> vns;

			private Rename(PerMap<String, String> vns) {
				this.vns = vns;
			}

			private Funp rename(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn0, value, expr, type) -> {
					var vn1 = newVarName.apply(vn0);
					var r1 = new Rename(vns.replace(vn0, vn1));
					return FunpDefine.of(vn1, rename(value), r1.rename(expr), type);
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs0, expr, type) -> {
					var pairs = Read.from2(pairs0);
					var vns1 = pairs.keys().fold(vns, (vns_, vn) -> vns_.replace(vn, newVarName.apply(vn)));
					var r1 = new Rename(vns1);
					return FunpDefineRec.of( //
							pairs.map2((vn, value) -> vns1.get(vn), (vn, value) -> r1.rename(value)).toList(), //
							r1.rename(expr), //
							type);
				})).applyIf(FunpLambda.class, f -> f.apply((vn0, expr, isCapture) -> {
					var vn1 = newVarName.apply(vn0);
					var r1 = new Rename(vns.replace(vn0, vn1));
					return FunpLambda.of(vn1, r1.rename(expr), isCapture);
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return FunpVariable.of(vns.get(var));
				})).result());
			}
		}

		return new Rename(PerMap.empty()).rename(node);
	}

	// Before - define i := memory ~ assign (i <= value)
	// After - define i := value
	private Funp inlineDefineAssigns(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n0 -> {
					var vns = new ArrayList<String>();
					FunpDoAssignVar assign;
					FunpTypeCheck check;
					FunpDefine define;

					while ((define = n0.cast(FunpDefine.class)) != null //
							&& define.type == Fdt.L_MONO //
							&& define.value instanceof FunpDontCare) {
						vns.add(define.vn);
						n0 = define.expr;
					}

					if ((check = n0.cast(FunpTypeCheck.class)) != null)
						n0 = check.expr;

					if ((assign = n0.cast(FunpDoAssignVar.class)) != null) {
						var vn = assign.var.vn;
						var n1 = assign.expr;
						var n2 = check != null ? FunpTypeCheck.of(check.left, check.right, n1) : n1;
						var b = false;

						for (var vn_ : List_.reverse(vns))
							if (!String_.equals(vn, vn_))
								n2 = FunpDefine.of(vn_, FunpDontCare.of(), n2, Fdt.L_MONO);
							else
								b = true;

						if (b)
							return FunpDefine.of(vn, assign.value, inline(n2), Fdt.L_MONO);
					}

					return null;
				});
			}
		}.inline(node);
	}

	// Before - define i := 1 ~ i + 1
	// After - 1 + 1
	// Before - expand i := 1 ~ i + i
	// After - 1 + 1
	private Funp inlineDefines(Funp node) {
		var defByVariables = Funp_.associateDefinitions(node);
		var countByDefs = new HashMap<Funp, IntMutable>();

		new Object() {
			public void count(Funp node_) {
				inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
					count(expr);
					return n_;
				})).applyIf(FunpReference.class, f -> f.apply(expr -> {
					countByDefs.computeIfAbsent(defByVariables.get(expr), v -> IntMutable.of(0)).update(9999);
					return null;
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					countByDefs.computeIfAbsent(defByVariables.get(f), v -> IntMutable.of(0)).increment();
					return null;
				})).result());
			}
		}.count(node);

		var zero = IntMutable.of(0);

		var defines = Read //
				.from2(defByVariables) //
				.values() //
				.distinct() //
				.filter(def -> def instanceof FunpDefine && countByDefs.getOrDefault(def, zero).value() <= 1) //
				.map2(def -> (FunpDefine) def) //
				.filterValue(def -> def.type == Fdt.L_MONO || def.type == Fdt.L_POLY) //
				.toMap();

		var expands = Read //
				.from2(defByVariables) //
				.mapValue(defines::get) //
				.filterValue(def -> def != null) //
				.toMap();

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpDefine define;
					if ((define = defines.get(n_)) != null)
						return inline(define.expr);
					else if ((define = expands.get(n_)) != null)
						return inline(define.value);
					else
						return null;
				});
			}
		}.inline(node);
	}

	// Before - define s := (struct (a 1, b 2, c 3,)) ~ s/c
	// After - 3
	private Funp inlineFields(Funp node) {
		var defs = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpField field;
					FunpStruct struct;
					FunpVariable variable;
					if ((field = n_.cast(FunpField.class)) != null //
							&& (variable = field.reference.expr.cast(FunpVariable.class)) != null //
							&& (struct = defs.get(variable).cast(FunpDefine.class, n -> n.value.cast(FunpStruct.class))) != null) {
						var pair = Read //
								.from2(struct.pairs) //
								.filterKey(field_ -> String_.equals(field_, field.field)) //
								.first();
						return pair != null ? inline(pair.t1) : null;
					} else
						return null;
				});
			}
		}.inline(node);
	}

	// Before - 3 | (i => i)
	// After - 3
	private Funp inlineLambdas(Funp node) {
		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw() //
						.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
							return lambda.cast(FunpLambda.class,
									l -> FunpDefine.of(l.vn, inline(value), inline(l.expr), Fdt.L_MONO));
						})) //
						.result());
			}
		}.inline(node);
	}

	// Before - define s := t:3 ~ if (`t:v` = s) then v else 0
	// After - 3
	private Funp inlineTags(Funp node) {
		var defs = Funp_.associateDefinitions(node);

		return new Object() {
			private Funp inline(Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> {
					FunpTag tag;
					FunpTagId tagId;
					FunpTagValue tagValue;
					FunpVariable variable;
					if ((tagId = n_.cast(FunpTagId.class)) != null //
							&& (variable = tagId.reference.expr.cast(FunpVariable.class)) != null //
							&& (tag = defs.get(variable).cast(FunpDefine.class, n -> n.value.cast(FunpTag.class))) != null)
						return FunpNumber.of(tag.id);
					else if ((tagValue = n_.cast(FunpTagValue.class)) != null //
							&& (variable = tagValue.reference.expr.cast(FunpVariable.class)) != null //
							&& (tag = defs.get(variable).cast(FunpDefine.class, n -> n.value.cast(FunpTag.class))) != null)
						return String_.equals(tag.tag, tagValue.tag) ? tag.value : FunpDontCare.of();
					else
						return null;
				});
			}
		}.inline(node);
	}

}
