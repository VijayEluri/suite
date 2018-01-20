package suite.primitive;

import suite.util.Fail;

public interface DblFlt_Int {

	public int apply(double c, float f);

	public default DblFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
