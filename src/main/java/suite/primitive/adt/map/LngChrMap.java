package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.LngChrSink;
import suite.primitive.LngChrSource;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Chr;
import suite.primitive.adt.pair.LngChrPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive char value. Character.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngChrMap {

	private static char EMPTYVALUE = ChrFunUtil.EMPTYVALUE;

	private int size;
	private long[] ks;
	private char[] vs;

	public static <T> Fun<Outlet<T>, LngChrMap> collect(Obj_Lng<T> kf0, Obj_Chr<T> vf0) {
		Obj_Lng<T> kf1 = kf0.rethrow();
		Obj_Chr<T> vf1 = vf0.rethrow();
		return outlet -> {
			LngChrMap map = new LngChrMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngChrMap() {
		this(8);
	}

	public LngChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(long key, Lng_Chr fun) {
		var v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngChrMap) {
			LngChrMap other = (LngChrMap) object;
			boolean b = size == other.size;
			for (LngObjPair<Character> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(LngChrSink sink) {
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		LngChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (LngObjPair<Character> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public char get(long key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : EMPTYVALUE;
	}

	public void put(long key, char v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Chr_Chr fun) {
		var mask = vs.length - 1;
		var index = index(key);
		var v0 = vs[index];
		var v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v_ = vs[index1];
					if (v_ != EMPTYVALUE) {
						var k = ks[index1];
						vs[index1] = EMPTYVALUE;
						rehash(index1);
						store(k, v_);
					}
				}
			}.rehash(index);
		rehash();
	}

	public int size() {
		return size;
	}

	public LngChrSource source() {
		return source_();
	}

	public LngObjStreamlet<Character> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(new LngObjSource<Character>() {
			private LngChrSource source0 = source_();
			private LngChrPair pair0 = LngChrPair.of((long) 0, (char) 0);

			public boolean source2(LngObjPair<Character> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			long[] ks0 = ks;
			char[] vs0 = vs;
			char v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(long key, char v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(long key) {
		var mask = vs.length - 1;
		var index = Long.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private LngChrSource source_() {
		return new LngChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngChrPair pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != EMPTYVALUE) {
						pair.update(k, v);
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new long[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
