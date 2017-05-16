package suite.adt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.streamlet.Read;
import suite.streamlet.Streamlet2;

public class ListMultimap<K, V> {

	private Map<K, List<V>> map;

	public ListMultimap() {
		this(new HashMap<>());
	}

	public ListMultimap(Map<K, List<V>> map) {
		this.map = map;
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public Streamlet2<K, V> entries() {
		return listEntries().concatMapValue(Read::from);
	}

	public List<V> get(K k) {
		List<V> list = map.get(k);
		return list != null ? list : Collections.emptyList();
	}

	public List<V> getMutable(K k) {
		return get_(k);
	}

	public boolean isEmpty() {
		for (List<V> value : map.values())
			if (!value.isEmpty())
				return false;
		return true;
	}

	public Streamlet2<K, List<V>> listEntries() {
		return Read.from2(map);
	}

	public void put(K k, V v) {
		get_(k).add(v);
	}

	public void remove(K k, V v) {
		get_(k).remove(v);
	}

	public int size() {
		int size = 0;
		for (List<V> value : map.values())
			size += value.size();
		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Pair<K, List<V>> e : listEntries())
			sb.append(e.t0 + "=" + e.t1 + ", ");
		sb.append("}");
		return sb.toString();
	}

	private List<V> get_(K k) {
		return map.computeIfAbsent(k, k_ -> new ArrayList<>());
	}

}
