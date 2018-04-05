package suite.primitive;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;

public class IntPrimitives {

	@FunctionalInterface
	public interface IntComparator {
		int compare(int c0, int c1);
	}

	@FunctionalInterface
	public interface Int_Obj<T> {
		public T apply(int c);

		public static <T> Fun<IntOutlet, Streamlet<T>> lift(Int_Obj<T> fun0) {
			Int_Obj<T> fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				int c;
				while ((c = s.next()) != IntFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Int_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return Fail.t("for " + i, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntObj_Obj<X, Y> {
		public Y apply(int c, X x);

		public default IntObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return Fail.t("for " + x + ":" + y, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntObjPredicate<T> {
		public boolean test(int c, T t);

		public default IntObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return Fail.t("for " + c + ":" + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(int c, T t);

		public default IntObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					Fail.t("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

	@FunctionalInterface
	public interface IntTest {
		public boolean test(int c);

		public default IntTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return Fail.t("for " + c, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntSink {
		public void sink(int c);

		public default IntSink rethrow() {
			return t -> {
				try {
					sink(t);
				} catch (Exception ex) {
					Fail.t("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface IntSource {
		public int source();
	}

	@FunctionalInterface
	public interface Obj_Int<T> {
		public int apply(T t);

		public static <T> Fun<Outlet<T>, IntStreamlet> lift(Obj_Int<T> fun0) {
			Obj_Int<T> fun1 = fun0.rethrow();
			return ts -> {
				var b = new IntsBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toInts().streamlet();
			};
		}

		public static <T> Obj_Int<Outlet<T>> sum(Obj_Int<T> fun0) {
			Obj_Int<T> fun1 = fun0.rethrow();
			return outlet -> {
				Source<T> source = outlet.source();
				T t;
				var result = (int) 0;
				while ((t = source.source()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Int<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return Fail.t("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> {
		public int apply(X x, Y y);

		public static <K, V> Obj_Int<Outlet2<K, V>> sum(ObjObj_Int<K, V> fun0) {
			ObjObj_Int<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				Pair<K, V> pair = Pair.of(null, null);
				Source2<K, V> source = outlet.source();
				var result = (int) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.t0, pair.t1);
				return result;
			};
		}

		public default ObjObj_Int<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return Fail.t("for " + x + ":" + y, ex);
				}
			};
		}
	}

}
