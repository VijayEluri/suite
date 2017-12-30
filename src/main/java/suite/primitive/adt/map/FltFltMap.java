package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.FltFltSink;
import suite.primitive.FltFltSource;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.pair.FltFltPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive float key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltFltMap {

	private int size;
	private float[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, FltFltMap> collect(Obj_Flt<T> kf0, Obj_Flt<T> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Obj_Flt<T> vf1 = vf0.rethrow();
		return outlet -> {
			FltFltMap map = new FltFltMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public FltFltMap() {
		this(8);
	}

	public FltFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(float key, Flt_Flt fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltFltMap) {
			FltFltMap other = (FltFltMap) object;
			boolean b = size == other.size;
			for (FltObjPair<Float> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(FltFltSink sink) {
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		FltFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (FltObjPair<Float> pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public float get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(float key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			float[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (FltObjPair<Float> pair : streamlet())
			sb.append(pair.t0 + ":" + pair.t1 + ",");
		return sb.toString();
	}

	public void update(float key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		size += ((vs[index] = fun.apply(v)) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v != FltFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public FltFltSource source() {
		return source_();
	}

	public FltObjStreamlet<Float> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(new FltObjSource<Float>() {
			private FltFltSource source0 = source_();
			private FltFltPair pair0 = FltFltPair.of((float) 0, (float) 0);

			public boolean source2(FltObjPair<Float> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private float put_(float key, float v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltFltSource source_() {
		return new FltFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltFltPair pair) {
				float v;
				while (index < capacity)
					if ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(ks[index++], v);
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
