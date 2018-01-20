package suite.primitive;

import suite.util.Fail;

public interface DblFlt_Lng {

	public long apply(double c, float f);

	public default DblFlt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
