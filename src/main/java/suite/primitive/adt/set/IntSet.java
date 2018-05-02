package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.Ints_;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;

/**
 * Set with intacter elements. Integer.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class IntSet {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private int[] vs;

	public static IntSet intersect(IntSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static IntSet union(IntSet... sets) {
		var set = new IntSet();
		for (var set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public IntSet() {
		this(8);
	}

	public IntSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(int c) {
		var capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(int c) {
		return vs[index(c)] == c;
	}

	public IntSet clone() {
		var capacity = vs.length;
		var set = new IntSet(capacity);
		set.size = size;
		Ints_.copy(vs, 0, set.vs, 0, capacity);
		return set;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntSet) {
			var other = (IntSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(IntSink sink) {
		var source = source_();
		int c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Integer.hashCode(c);
		return h;
	}

	public boolean remove(int c) {
		var mask = vs.length - 1;
		var index = index(c);
		var b = vs[index] == c;
		if (b) {
			vs[index] = EMPTYVALUE;
			size--;
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != EMPTYVALUE) {
						vs[index1] = EMPTYVALUE;
						rehash(index1);
						vs[index(v)] = v;
					}
				}
			}.rehash(index);
		}
		return b;
	}

	public IntSource source() {
		return source_();
	}

	public IntStreamlet streamlet() {
		return new IntStreamlet(() -> IntOutlet.of(source_()));
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(int c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(int c) {
		var mask = vs.length - 1;
		var index = Integer.hashCode(c) & mask;
		int c0;
		while ((c0 = vs[index]) != EMPTYVALUE && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private IntSource source_() {
		return new IntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public int source() {
				int v;
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
