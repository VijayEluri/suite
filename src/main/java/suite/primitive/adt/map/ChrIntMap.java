package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrIntSink;
import suite.primitive.ChrIntSource;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Int;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.ChrIntPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrIntMap {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private char[] ks;
	private int[] vs;

	public static <T> Fun<Outlet<T>, ChrIntMap> collect(Obj_Chr<T> kf0, Obj_Int<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Int<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrIntMap map = new ChrIntMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrIntMap() {
		this(8);
	}

	public ChrIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(char key, Chr_Int fun) {
		var v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrIntMap) {
			ChrIntMap other = (ChrIntMap) object;
			boolean b = size == other.size;
			for (ChrObjPair<Integer> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(ChrIntSink sink) {
		ChrIntPair pair = ChrIntPair.of((char) 0, (int) 0);
		ChrIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (ChrObjPair<Integer> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public int get(char key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : EMPTYVALUE;
	}

	public void put(char key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(char key, Int_Int fun) {
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

	public ChrIntSource source() {
		return source_();
	}

	public ChrObjStreamlet<Integer> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Integer>() {
			private ChrIntSource source0 = source_();
			private ChrIntPair pair0 = ChrIntPair.of((char) 0, (int) 0);

			public boolean source2(ChrObjPair<Integer> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			char[] ks0 = ks;
			var vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(char key, int v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(char key) {
		var mask = vs.length - 1;
		var index = Character.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private ChrIntSource source_() {
		return new ChrIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrIntPair pair) {
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
		ks = new char[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
