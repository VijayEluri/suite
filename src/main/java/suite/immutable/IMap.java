package suite.immutable;

import java.util.Iterator;

import suite.streamlet.Streamlet;
import suite.util.Pair;

public class IMap<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {

	private ITree<Pair<K, V>> tree = new I23Tree<>(Pair.<K, V> comparatorByFirst());

	public IMap() {
	}

	public IMap(ITree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return stream().iterator();
	}

	public Streamlet<Pair<K, V>> stream() {
		return tree.stream();
	}

	public V get(K k) {
		return Pair.second(tree.find(Pair.of(k, (V) null)));
	}

	public IMap<K, V> put(K k, V v) {
		return new IMap<>(tree.add(Pair.of(k, v)));
	}

	public IMap<K, V> replace(K k, V v) {
		return new IMap<>(tree.replace(Pair.of(k, v)));
	}

	public IMap<K, V> remove(K k) {
		return new IMap<>(tree.remove(Pair.of(k, (V) null)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		for (Pair<K, V> pair : this)
			sb.append(pair.t0 + " = " + pair.t1 + ", ");

		sb.append("}");
		return sb.toString();
	}

}
