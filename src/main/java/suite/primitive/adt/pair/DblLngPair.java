package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.DblFunUtil;
import suite.primitive.DblLng_Obj;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class DblLngPair {

	private static DblLngPair none_ = DblLngPair.of(DblFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public double t0;
	public long t1;

	public static Iterate<DblLngPair> map0(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<DblLngPair> map1(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static DblLngPair none() {
		return none_;
	}

	public static DblLngPair of(double t0, long t1) {
		return new DblLngPair(t0, t1);
	}

	private DblLngPair(double t0, long t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<DblLngPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<DblLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static double first_(DblLngPair pair) {
		return pair.t0;
	}

	public static long second(DblLngPair pair) {
		return pair.t1;
	}

	public <O> Opt<O> map(DblLng_Obj<O> fun) {
		return t0 != DblFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public void update(double t0_, long t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblLngPair.class) {
			DblLngPair other = (DblLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
