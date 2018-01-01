package suite.primitive.adt.map;

import java.util.Objects;

import suite.primitive.DblPrimitives.DblObjSink;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.util.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class DblObjMap<V> {

	private int size;
	private double[] ks;
	private Object[] vs;

	public static <V> DblObjMap<V> collect(DblObjOutlet<V> outlet) {
		DblObjMap<V> map = new DblObjMap<>();
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public DblObjMap() {
		this(8);
	}

	public DblObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(double key, Dbl_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblObjMap) {
			DblObjMap<?> other = (DblObjMap<?>) object;
			boolean b = size == other.size;
			for (DblObjPair<V> pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(DblObjSink<V> sink) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		DblObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (DblObjPair<V> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public V put(double key, V v1) {
		size++;
		V v0 = cast(store(key, v1));
		rehash();
		return v0;
	}

	public void update(double key, Iterate<V> fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		size += ((vs[index] = fun.apply(cast(v0))) != null ? 1 : 0) - (v0 != null ? 1 : 0);
		rehash();
	}

	public int size() {
		return size;
	}

	public DblObjSource<V> source() {
		return source_();
	}

	public DblObjStreamlet<V> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (DblObjPair<V> pair : streamlet())
			sb.append(pair.t0 + ":" + pair.t1 + ",");
		return sb.toString();
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			double[] ks0 = ks;
			Object[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				Object o = vs0[i];
				if (o != null)
					store(ks0[i], o);
			}
		}
	}

	private Object store(double key, Object v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblObjSource<V> source_() {
		return new DblObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblObjPair<V> pair) {
				Object v;
				while (index < capacity)
					if ((v = vs[index]) == null)
						index++;
					else {
						pair.update(ks[index++], cast(v));
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
