package suite.object;

import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class ToStringObject<T extends ToStringObject<T>> implements Cloneable, AutoInterface<T> {

	private static Inspect inspect = Singleton.me.inspect;

	@Override
	public String toString() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return new ObjectSupport<>(inspect::values).toString(t);
	}

}