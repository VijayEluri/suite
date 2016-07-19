package suite.node;

import suite.util.Util;

public class Str extends Node {

	public final String value;

	public Str(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Str.class) {
			Str str = (Str) object;
			return Util.stringEquals(value, str.value);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
