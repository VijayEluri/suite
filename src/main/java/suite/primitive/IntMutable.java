package suite.primitive;

import suite.util.Fail;
import suite.util.Object_;

/**
 * An indirect reference to a primitive int. Integer.MIN_VALUE is not allowed
 * in the value.
 * 
 * @author ywsing
 */
public class IntMutable {

	private int value;

	public static IntMutable nil() {
		return IntMutable.of(IntFunUtil.EMPTYVALUE);
	}

	public static IntMutable of(int c) {
		IntMutable p = new IntMutable();
		p.update(c);
		return p;
	}

	public int increment() {
		return value++;
	}

	public void set(int c) {
		if (value == IntFunUtil.EMPTYVALUE)
			update(c);
		else
			Fail.t("value already set");
	}

	public void update(int c) {
		value = c;
	}

	public int get() {
		return value;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IntMutable.class && value == ((IntMutable) object).value;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(value);
	}

	@Override
	public String toString() {
		return value != IntFunUtil.EMPTYVALUE ? Integer.toString(value) : "null";
	}

}
