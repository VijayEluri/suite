package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrDblPredicate {

	public boolean test(char c, double f);

	public default ChrDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
