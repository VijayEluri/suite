package suite.primitive; import static suite.util.Friends.fail;

public interface ChrFlt_Int {

	public int apply(char c, float f);

	public default ChrFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
