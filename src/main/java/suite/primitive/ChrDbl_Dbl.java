package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrDbl_Dbl {

	public double apply(char c, double f);

	public default ChrDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
