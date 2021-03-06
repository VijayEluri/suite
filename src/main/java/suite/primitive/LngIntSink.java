package suite.primitive;

import static suite.util.Friends.fail;

public interface LngIntSink {

	public void sink2(long c, int f);

	public default LngIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
