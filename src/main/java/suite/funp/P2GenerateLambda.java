package suite.funp;

import suite.adt.Mutable;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIterate;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.Switch;

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
			Switch<Thunk> sw = new Switch<>(n0);

			sw.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				Thunk lambda1 = compile_(lambda);
				Thunk value1 = compile_(value);
				return rt -> ((Fun_) lambda1.apply(rt)).apply(value1.apply(rt));
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				Streamlet<Thunk> thunks = Read.from(elements).map(element -> compile_(element));
				return rt -> new Vec(thunks.map(thunk -> thunk.apply(rt)).toArray(Value.class));
			})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
				Bool b1 = new Bool(b);
				return rt -> b1;
			})).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
				return compile_(FunpApply.of(value, FunpLambda.of(var, expr)));
			})).applyIf(FunpDefineRec.class, f -> {
				throw new RuntimeException();
			}).applyIf(FunpDeref.class, f -> {
				throw new RuntimeException();
			}).applyIf(FunpError.class, f -> {
				throw new RuntimeException();
			}).applyIf(FunpFixed.class, f -> f.apply((var, expr) -> {
				int fs1 = fs + 1;
				Thunk thunk = compile(fs1, env.replace(var, fs1), expr);
				return rt -> {
					Mutable<Fun_> mut = Mutable.nil();
					Fun_ fun = p -> thunk.apply(new Rt(rt, mut.get()));
					mut.set(fun);
					return fun;
				};
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				Thunk if1 = compile_(if_);
				Thunk then1 = compile_(then);
				Thunk else1 = compile_(else_);
				return rt -> (b(rt, if1) ? then1 : else1).apply(rt);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				Thunk array = compile_(FunpDeref.of(reference));
				Thunk index1 = compile_(index);
				return rt -> ((Vec) array.apply(rt)).values[i(rt, index1)];
			})).applyIf(FunpIterate.class, f -> f.apply((var, init, cond, iterate) -> {
				int fs1 = fs + 1;
				IMap<String, Integer> env1 = env.replace(var, fs1);
				Thunk init_ = compile_(init);
				Thunk cond_ = compile(fs1, env1, cond);
				Thunk iterate_ = compile(fs1, env1, iterate);
				return rt -> {
					Rt rt1 = new Rt(rt, init_.apply(rt));
					while (b(rt1, cond_))
						rt1.var = iterate_.apply(rt1);
					return rt1.var;
				};
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				int fs1 = fs + 1;
				Thunk thunk = compile(fs1, env.replace(var, fs1), expr);
				return rt -> (Fun_) p -> thunk.apply(new Rt(rt, p));
			})).applyIf(FunpNumber.class, f -> f.apply(i -> {
				Int i1 = new Int(i.get());
				return rt -> i1;
			})).applyIf(FunpReference.class, f -> {
				throw new RuntimeException();
			}).applyIf(FunpTree.class, f -> f.apply((operator, left, right) -> {
				Thunk v0 = compile_(left);
				Thunk v1 = compile_(right);
				if (operator == TermOp.BIGAND)
					return rt -> new Bool(b(rt, v0) && b(rt, v1));
				else if (operator == TermOp.BIGOR_)
					return rt -> new Bool(b(rt, v0) || b(rt, v1));
				else {
					IntInt_Int fun = TreeUtil.evaluateOp(operator);
					return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
				}
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				int fd = fs - env.get(var);
				return rt -> {
					for (int i = 0; i < fd; i++)
						rt = rt.parent;
					return rt.var;
				};
			}));

			return sw.nonNullResult();
		}
	}

	private static int i(Rt rt, Thunk value) {
		return ((Int) value.apply(rt)).i;
	}

	private static boolean b(Rt rt, Thunk value) {
		return ((Bool) value.apply(rt)).b;
	}

}