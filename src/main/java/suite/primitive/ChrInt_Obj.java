package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrInt_Obj<T> {

	public T apply(char c, int f);

	public default ChrInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
