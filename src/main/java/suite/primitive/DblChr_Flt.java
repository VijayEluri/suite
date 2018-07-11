package suite.primitive; import static suite.util.Friends.fail;

public interface DblChr_Flt {

	public float apply(double c, char f);

	public default DblChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
