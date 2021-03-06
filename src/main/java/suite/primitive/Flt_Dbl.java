package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Dbl {

	public double apply(float c);

	public static Fun<FltOutlet, DblStreamlet> lift(Flt_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<FltOutlet> sum(Flt_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			float c;
			var result = (double) 0;
			while ((c = source.g()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
