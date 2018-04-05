package suite.primitive;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;

public class LngPrimitives {

	@FunctionalInterface
	public interface LngComparator {
		int compare(long c0, long c1);
	}

	@FunctionalInterface
	public interface Lng_Obj<T> {
		public T apply(long c);

		public static <T> Fun<LngOutlet, Streamlet<T>> lift(Lng_Obj<T> fun0) {
			Lng_Obj<T> fun1 = fun0.rethrow();
			return s -> {
				List<T> ts = new ArrayList<>();
				long c;
				while ((c = s.next()) != LngFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Lng_Obj<T> rethrow() {
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
	public interface LngObj_Obj<X, Y> {
		public Y apply(long c, X x);

		public default LngObj_Obj<X, Y> rethrow() {
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
	public interface LngObjPredicate<T> {
		public boolean test(long c, T t);

		public default LngObjPredicate<T> rethrow() {
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
	public interface LngObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(long c, T t);

		public default LngObjSink<T> rethrow() {
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
	public interface LngObjSource<T> {
		public boolean source2(LngObjPair<T> pair);
	}

	@FunctionalInterface
	public interface LngTest {
		public boolean test(long c);

		public default LngTest rethrow() {
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
	public interface LngSink {
		public void sink(long c);

		public default LngSink rethrow() {
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
	public interface LngSource {
		public long source();
	}

	@FunctionalInterface
	public interface Obj_Lng<T> {
		public long apply(T t);

		public static <T> Fun<Outlet<T>, LngStreamlet> lift(Obj_Lng<T> fun0) {
			Obj_Lng<T> fun1 = fun0.rethrow();
			return ts -> {
				var b = new LongsBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toLongs().streamlet();
			};
		}

		public static <T> Obj_Lng<Outlet<T>> sum(Obj_Lng<T> fun0) {
			Obj_Lng<T> fun1 = fun0.rethrow();
			return outlet -> {
				Source<T> source = outlet.source();
				T t;
				var result = (long) 0;
				while ((t = source.source()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Lng<T> rethrow() {
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
	public interface ObjObj_Lng<X, Y> {
		public long apply(X x, Y y);

		public static <K, V> Obj_Lng<Outlet2<K, V>> sum(ObjObj_Lng<K, V> fun0) {
			ObjObj_Lng<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				Pair<K, V> pair = Pair.of(null, null);
				Source2<K, V> source = outlet.source();
				var result = (long) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.t0, pair.t1);
				return result;
			};
		}

		public default ObjObj_Lng<X, Y> rethrow() {
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
