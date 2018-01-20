package suite.primitive;

import suite.util.Fail;

public interface FltLngSink {

	public void sink2(float c, long f);

	public default FltLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
