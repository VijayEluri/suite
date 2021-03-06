package suite.primitive;

import static suite.util.Friends.fail;

public interface LngInt_Obj<T> {

	public T apply(long c, int f);

	public default LngInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
