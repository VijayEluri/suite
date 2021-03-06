package suite.primitive;

import static suite.util.Friends.fail;

public interface FltDbl_Dbl {

	public double apply(float c, double f);

	public default FltDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
