package suite.adt.set;

import java.util.Arrays;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.streamlet.LngOutlet;
import suite.streamlet.LngStreamlet;

/**
 * Set with longacter elements. Long.MIN_VALUE is not allowed. Not thread-safe.
 *
 * @author ywsing
 */
public class LngSet {

	private int size;
	private long[] vs;

	public LngSet() {
		this(8);
	}

	public LngSet(int capacity) {
		allocate(capacity);
	}

	public void forEach(LngSink sink) {
		LngSource source = source_();
		long c;
		while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	public boolean add(long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(v);
	}

	public LngSource source() {
		return source_();
	}

	public LngStreamlet stream() {
		return new LngStreamlet(() -> LngOutlet.of(source_()));
	}

	private boolean add_(long v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(v1) & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (v0 != v1)
				index = index + 1 & mask;
			else
				return false;
		vs[index] = v1;
		return true;
	}

	private LngSource source_() {
		return new LngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public long source() {
				long v;
				while ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return LngFunUtil.EMPTYVALUE;
				return v;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
