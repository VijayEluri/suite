package suite.node;

import java.util.Objects;

import suite.util.Util;

public class Data<T> extends Node {

	public final T data;

	public Data(T data) {
		this.data = data;
	}

	public static <T> T get(Node node) {
		@SuppressWarnings("unchecked")
		T t = (T) ((Data<?>) node).data;
		return t;
	}

	@Override
	public String toString() {
		return "Data [" + (data != null ? data.toString() : "null") + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(data);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			return Util.clazz(node) == Data.class && Objects.equals(data, ((Data<?>) node).data);
		} else
			return false;
	}

}
