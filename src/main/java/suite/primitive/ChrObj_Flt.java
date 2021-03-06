package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrObj_Flt<T> {

	public float apply(char c, T t);

	public default ChrObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
