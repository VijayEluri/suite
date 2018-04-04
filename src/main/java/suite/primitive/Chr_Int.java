package suite.primitive;

import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Int {

	public int apply(char c);

	public static Fun<ChrOutlet, IntStreamlet> lift(Chr_Int fun0) {
		Chr_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<ChrOutlet> sum(Chr_Int fun0) {
		Chr_Int fun1 = fun0.rethrow();
		return outlet -> {
			ChrSource source = outlet.source();
			char c;
			var result = (int) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
