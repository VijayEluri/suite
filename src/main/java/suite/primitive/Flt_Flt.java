package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Flt {

	public float apply(float c);

	public static Fun<FltOutlet, FltStreamlet> lift(Flt_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<FltOutlet> sum(Flt_Flt fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			float c;
			var result = (float) 0;
			while ((c = source.g()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
