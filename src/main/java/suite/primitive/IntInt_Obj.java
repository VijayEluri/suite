package suite.primitive;

import static suite.util.Friends.fail;

public interface IntInt_Obj<T> {

	public T apply(int c, int f);

	public default IntInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
