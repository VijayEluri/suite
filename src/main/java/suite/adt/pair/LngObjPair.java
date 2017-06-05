package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class LngObjPair<T> {

	public long t0;
	public T t1;

	public static <V> Fun<LngObjPair<V>, LngObjPair<V>> map0(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<LngObjPair<V0>, LngObjPair<V1>> map1(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static <T> LngObjPair<T> of(long t0, T t1) {
		return new LngObjPair<>(t0, t1);
	}

	private LngObjPair(long t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<LngObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T> Comparator<LngObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long first_(LngObjPair<?> pair) {
		return pair.t0;
	}

	public static <T> T second(LngObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngObjPair.class) {
			LngObjPair<?> other = (LngObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
