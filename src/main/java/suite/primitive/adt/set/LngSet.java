package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;

/**
 * Set with longacter elements. Long.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class LngSet {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

	private int size;
	private long[] vs;

	public static LngSet intersect(LngSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static LngSet union(LngSet... sets) {
		var set = new LngSet();
		for (var set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public LngSet() {
		this(8);
	}

	public LngSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(long c) {
		var capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			long v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(long c) {
		return vs[index(c)] == c;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngSet) {
			var other = (LngSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(LngSink sink) {
		var source = source_();
		long c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Long.hashCode(c);
		return h;
	}

	public LngSource source() {
		return source_();
	}

	public LngStreamlet streamlet() {
		return new LngStreamlet(() -> LngOutlet.of(source_()));
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(long c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(long c) {
		var mask = vs.length - 1;
		var index = Long.hashCode(c) & mask;
		long c0;
		while ((c0 = vs[index]) != EMPTYVALUE && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private LngSource source_() {
		return new LngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public long source() {
				long v;
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
