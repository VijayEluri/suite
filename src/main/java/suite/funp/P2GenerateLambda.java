package suite.funp;

import java.util.Map;

import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineGlobal;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpFold;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpIoCat;
import suite.funp.P0.FunpIoFold;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.To;
import suite.util.Util;

public class P2GenerateLambda {

	public static class Rt {
		private Rt parent;
		private Value var;

		public Rt(Rt parent, Value var) {
			this.parent = parent;
			this.var = var;
		}
	}

	public interface Value {
	}

	public static class Bool implements Value {
		public final boolean b;

		private Bool(boolean b) {
			this.b = b;
		}
	}

	public static class Int implements Value {
		public final int i;

		private Int(int i) {
			this.i = i;
		}
	}

	public static class Ref implements Value {
		public final Value v;

		private Ref(Value v) {
			this.v = v;
		}
	}

	public static class Struct implements Value {
		public final Map<String, Value> map;

		public Struct(Map<String, Value> map) {
			this.map = map;
		}
	}

	public static class Vec implements Value {
		public final Value[] values;

		private Vec(Value[] values) {
			this.values = values;
		}
	}

	public interface Thunk extends Fun<Rt, Value>, Value {
	}

	private interface Fun_ extends Fun<Value, Value>, Value {
	}

	public Thunk compile(int fs, IMap<String, Integer> env, Funp n) {
		return new Compile(fs, env).compile_(n);
	}

	private class Compile {
		private int fs;
		private IMap<String, Integer> env;

		private Compile(int fs, IMap<String, Integer> env) {
			this.fs = fs;
			this.env = env;
		}

		private Thunk compile_(Funp n0) {
			return n0.<Thunk> switch_( //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var lambda1 = compile_(lambda);
				var value1 = compile_(value);
				return rt -> ((Fun_) lambda1.apply(rt)).apply(value1.apply(rt));
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var thunks = Read.from(elements).map(this::compile_);
				return rt -> new Vec(thunks.map(thunk -> thunk.apply(rt)).toArray(Value.class));
			})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
				var b1 = new Bool(b);
				return rt -> b1;
			})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
				return compile_(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
				return compile_(FunpApply.of(value, FunpLambda.of(var, expr)));
			})).applyIf(FunpDefineGlobal.class, f -> f.apply((var, value, expr) -> {
				return Fail.t();
			})).applyIf(FunpDefineRec.class, f -> {
				return Fail.t();
			}).applyIf(FunpDeref.class, f -> {
				var p = compile_(f);
				return rt -> ((Ref) p.apply(rt)).v;
			}).applyIf(FunpDontCare.class, f -> {
				return rt -> new Int(0);
			}).applyIf(FunpError.class, f -> {
				return rt -> Fail.t();
			}).applyIf(FunpField.class, f -> f.apply((ref, field) -> {
				var p = compile_(ref);
				return rt -> ((Struct) ((Ref) p.apply(rt)).v).map.get(field);
			})).applyIf(FunpFold.class, f -> f.apply((init, cont, next) -> {
				return fold(init, cont, next);
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				var if1 = compile_(if_);
				var then1 = compile_(then);
				var else1 = compile_(else_);
				return rt -> (b(rt, if1) ? then1 : else1).apply(rt);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var array = compile_(FunpDeref.of(reference));
				var index1 = compile_(index);
				return rt -> ((Vec) array.apply(rt)).values[i(rt, index1)];
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return compile_(expr);
			})).applyIf(FunpIoCat.class, f -> f.apply(expr -> {
				return Fail.t();
			})).applyIf(FunpIoFold.class, f -> f.apply((init, cont, next) -> {
				return fold(init, cont, next);
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				var fs1 = fs + 1;
				var thunk = compile(fs1, env.replace(var, fs1), expr);
				return rt -> (Fun_) p -> thunk.apply(new Rt(rt, p));
			})).applyIf(FunpNumber.class, f -> f.apply(i -> {
				var i1 = new Int(i.get());
				return rt -> i1;
			})).applyIf(FunpPredefine.class, f -> f.apply(expr -> {
				return compile_(expr);
			})).applyIf(FunpReference.class, f -> {
				var v = compile_(f);
				return rt -> new Ref(v.apply(rt));
			}).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				var expr_ = compile_(expr);
				return rt -> new Vec(To.array(count, Value.class, i -> expr_.apply(rt)));
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				var funs = Read.from2(pairs).mapValue(this::compile_).collect(As::streamlet2);
				return rt -> new Struct(funs.mapValue(v -> v.apply(rt)).toMap());
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
				var v0 = compile_(lhs);
				var v1 = compile_(rhs);
				if (op == TermOp.BIGAND)
					return rt -> new Bool(b(rt, v0) && b(rt, v1));
				else if (op == TermOp.BIGOR_)
					return rt -> new Bool(b(rt, v0) || b(rt, v1));
				else {
					var fun = TreeUtil.evaluateOp(op);
					return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
				}
			})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
				var v0 = compile_(lhs);
				var v1 = compile_(rhs);
				var fun = TreeUtil.evaluateOp(op);
				return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				var fd = fs - env.get(var);
				return rt -> {
					for (var i = 0; i < fd; i++)
						rt = rt.parent;
					return rt.var;
				};
			})).nonNullResult();
		}

		private Thunk fold(Funp init, Funp cont, Funp next) {
			var var = "fold" + Util.temp();
			var fs1 = fs + 1;
			var init_ = compile_(init);
			var var_ = FunpVariable.of(var);
			var compile1 = new Compile(fs1, env.replace(var, fs1));
			var cont_ = compile1.compile_(FunpApply.of(var_, cont));
			var next_ = compile1.compile_(FunpApply.of(var_, next));
			return rt -> {
				var rt1 = new Rt(rt, init_.apply(rt));
				while (b(rt1, cont_))
					rt1.var = next_.apply(rt1);
				return rt1.var;
			};
		}
	}

	private static int i(Rt rt, Thunk value) {
		return ((Int) value.apply(rt)).i;
	}

	private static boolean b(Rt rt, Thunk value) {
		return ((Bool) value.apply(rt)).b;
	}

}
