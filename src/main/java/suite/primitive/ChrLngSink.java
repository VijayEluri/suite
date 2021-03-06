package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrLngSink {

	public void sink2(char c, long f);

	public default ChrLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
