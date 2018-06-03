package suite.util;

import java.lang.reflect.Field;
import java.util.HashMap;

import suite.adt.IdentityKey;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public abstract class AutoObject<T extends AutoObject<T>> implements Cloneable, Comparable<T>, AutoInterface<T> {

	private static Inspect inspect = Singleton.me.inspect;

	@Override
	public AutoObject<T> clone() {
		var map = new HashMap<IdentityKey<?>, AutoObject<?>>();

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (AutoObject<T>) new Object() {
				private AutoObject<?> clone(AutoObject<?> t0) throws IllegalAccessException {
					var key = IdentityKey.of(t0);
					var tx = map.get(key);
					if (tx == null) {
						map.put(key, tx = Object_.new_(t0.getClass()));
						var t1 = (AutoObject<T>) tx;
						for (var field : t0.fields_()) {
							var v0 = field.get(t0);
							var v1 = v0 instanceof AutoObject ? clone((AutoObject<?>) v0) : v0;
							field.set(t1, v1);
						}
					}
					return tx;
				}
			}.clone(this);

			return object;
		});
	}

	@Override
	public int compareTo(T t1) {
		return autoObject().compare(self(), t1);
	}

	@Override
	public boolean equals(Object object) {
		return autoObject().isEquals(self(), object);
	}

	public Streamlet<Field> fields() {
		return fields_();
	}

	@Override
	public int hashCode() {
		return autoObject().hashCode(self());
	}

	@Override
	public String toString() {
		return autoObject().toString(self());
	}

	private AutoObject_<T> autoObject() {
		return new AutoObject_<>(inspect::values);
	}

	private Streamlet<Field> fields_() {
		return Read.from(inspect.fields(getClass()));
	}

	private T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

}
