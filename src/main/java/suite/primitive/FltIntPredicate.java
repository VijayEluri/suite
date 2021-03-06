package suite.primitive;

import static suite.util.Friends.fail;

public interface FltIntPredicate {

	public boolean test(float c, int f);

	public default FltIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
