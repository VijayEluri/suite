package suite.persistent;

import java.util.Iterator;
import java.util.Objects;

import suite.object.Object_;
import suite.streamlet.Streamlet;

public class PerSet<V extends Comparable<V>> implements Iterable<V> {

	private static PerSet<?> empty = new PerSet<Integer>();
	private PerTree<V> tree = new PbTree<>(Object_::compare);

	public static <V extends Comparable<V>> PerSet<V> empty() {
		@SuppressWarnings("unchecked")
		PerSet<V> m = (PerSet<V>) empty;
		return m;
	}

	private PerSet() {
	}

	public PerSet(PerTree<V> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<V> iterator() {
		return streamlet().iterator();
	}

	public Streamlet<V> streamlet() {
		return tree.streamlet();
	}

	public boolean contains(V v) {
		return tree.find(v) != null;
	}

	public V get(V v) {
		return tree.find(v);
	}

	public PerSet<V> add(V v) {
		return new PerSet<>(tree.add(v));
	}

	public PerSet<V> replace(V v) {
		return new PerSet<>(tree.replace(v));
	}

	public PerSet<V> remove(V v) {
		return new PerSet<>(tree.remove(v));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == PerSet.class && Objects.equals(streamlet(), ((PerSet<?>) object).streamlet());
	}

	@Override
	public int hashCode() {
		return tree.streamlet().hashCode();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("(");

		for (var v : this)
			sb.append(v + ", ");

		sb.append(")");
		return sb.toString();
	}

}
