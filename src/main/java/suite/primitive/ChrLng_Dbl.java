package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrLng_Dbl {

	public double apply(char c, long f);

	public default ChrLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
