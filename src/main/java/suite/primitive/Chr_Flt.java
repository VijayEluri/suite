package suite.primitive;

import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Flt {

	public float apply(char c);

	public static Fun<ChrOutlet, FltStreamlet> lift(Chr_Flt fun0) {
		Chr_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<ChrOutlet> sum(Chr_Flt fun0) {
		Chr_Flt fun1 = fun0.rethrow();
		return outlet -> {
			ChrSource source = outlet.source();
			char c;
			var result = (float) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
