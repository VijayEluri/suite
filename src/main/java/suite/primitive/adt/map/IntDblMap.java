package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntDblSink;
import suite.primitive.IntDblSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.IntDblPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntDblMap {

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private int size;
	private int[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, IntDblMap> collect(Obj_Int<T> kf0, Obj_Dbl<T> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return outlet -> {
			var map = new IntDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntDblMap() {
		this(8);
	}

	public IntDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(int key, Int_Dbl fun) {
		var v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntDblMap) {
			var other = (IntDblMap) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntDblSink sink) {
		var pair = IntDblPair.of((int) 0, (double) 0);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(int key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : EMPTYVALUE;
	}

	public void put(int key, double v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Dbl_Dbl fun) {
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

	public IntDblSource source() {
		return source_();
	}

	public IntObjStreamlet<Double> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Double>() {
			private IntDblSource source0 = source_();
			private IntDblPair pair0 = IntDblPair.of((int) 0, (double) 0);

			public boolean source2(IntObjPair<Double> pair) {
				var b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var ks0 = ks;
			var vs0 = vs;
			double v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(int key, double v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(int key) {
		var mask = vs.length - 1;
		var index = Integer.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private IntDblSource source_() {
		return new IntDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntDblPair pair) {
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
		ks = new int[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
