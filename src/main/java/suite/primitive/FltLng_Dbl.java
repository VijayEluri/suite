package suite.primitive;

import suite.util.Fail;

public interface FltLng_Dbl {

	public double apply(float c, long f);

	public default FltLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
