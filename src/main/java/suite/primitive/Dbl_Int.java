package suite.primitive;

import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Int {

	public int apply(double c);

	public static Fun<DblOutlet, IntStreamlet> lift(Dbl_Int fun0) {
		Dbl_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<DblOutlet> sum(Dbl_Int fun0) {
		Dbl_Int fun1 = fun0.rethrow();
		return outlet -> {
			DblSource source = outlet.source();
			double c;
			var result = (int) 0;
			while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
