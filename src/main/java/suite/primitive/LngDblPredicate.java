package suite.primitive;

public interface LngDblPredicate {

	public boolean test(long c, double f);

	public default LngDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
