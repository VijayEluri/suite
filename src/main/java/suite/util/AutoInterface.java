package suite.util;

import suite.util.FunUtil.Fun;

public interface AutoInterface<T> {

	public default <U extends T, V> V cast(Class<U> clazz, Fun<U, V> fun) {
		U u = cast(clazz);
		return u != null ? fun.apply(u) : null;
	}

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

}
