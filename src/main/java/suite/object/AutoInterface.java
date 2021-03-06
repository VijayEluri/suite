package suite.object;

import suite.streamlet.FunUtil.Fun;
import suite.util.Switch;

public interface AutoInterface<T> {

	public default <U extends T, V> V cast(Class<U> clazz, Fun<U, V> fun) {
		var u = cast(clazz);
		return u != null ? fun.apply(u) : null;
	}

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

	public default Switch<T> sw() {
		return switch_();
	}

	public default <U> Switch<U> switch_() {
		return new Switch<>(this);
	}

}
