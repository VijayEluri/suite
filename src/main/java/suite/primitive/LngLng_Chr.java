package suite.primitive;

import static suite.util.Friends.fail;

public interface LngLng_Chr {

	public char apply(long c, long f);

	public default LngLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
