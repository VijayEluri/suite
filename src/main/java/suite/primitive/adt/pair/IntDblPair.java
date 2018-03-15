package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntDbl_Obj;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class IntDblPair {

	private static IntDblPair none_ = IntDblPair.of(IntFunUtil.EMPTYVALUE, DblFunUtil.EMPTYVALUE);

	public int t0;
	public double t1;

	public static Iterate<IntDblPair> map0(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<IntDblPair> map1(Dbl_Dbl fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntDblPair none() {
		return none_;
	}

	public static IntDblPair of(int t0, double t1) {
		return new IntDblPair(t0, t1);
	}

	private IntDblPair(int t0, double t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<IntDblPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Double.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntDblPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int first_(IntDblPair pair) {
		return pair.t0;
	}

	public static double second(IntDblPair pair) {
		return pair.t1;
	}

	public <O> O apply(IntDbl_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(int t0_, double t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntDblPair.class) {
			IntDblPair other = (IntDblPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Double.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
