package suite.immutable;

import java.util.Objects;

import suite.util.Object_;

/**
 * A list of nodes that can be easily expanded in left or right direction.
 */
public class IVector<T> {

	@SuppressWarnings("unchecked")
	public T[] emptyArray = (T[]) new Object[0];
	public IVector<T> empty = new IVector<>(emptyArray);

	private Data<T> data;
	private int start, end;

	private static class Data<T> {
		private T[] nodes;
		private int startUsed, endUsed;

		private Data() {
			this(16);
		}

		private Data(int len) {
			this(len, len * 3 / 4);
		}

		private Data(int len, int startUsed) {
			@SuppressWarnings("unchecked")
			T[] array = (T[]) new Object[len];
			nodes = array;
			endUsed = this.startUsed = startUsed;
		}

		private void insertBefore(T n) {
			nodes[--startUsed] = n;
		}

		private void insertAfter(T n) {
			nodes[++endUsed] = n;
		}

		private void insertBefore(T[] n, int s, int e) {
			int l1 = e - s;
			startUsed -= l1;
			System.arraycopy(n, s, nodes, startUsed, l1);
		}

		private void insertAfter(T[] n, int s, int e) {
			int l1 = e - s;
			System.arraycopy(n, s, nodes, endUsed, l1);
			endUsed += l1;
		}
	}

	public IVector(T node) {
		this.data = new Data<>();
		data.insertBefore(node);
	}

	public IVector(T[] nodes) {
		this.data = new Data<>();
		data.insertBefore(nodes, 0, nodes.length);
	}

	private IVector(Data<T> data, int start, int end) {
		this.data = data;
		this.start = start;
		this.end = end;
	}

	public IVector<T> cons(T n, IVector<T> v) {
		int vlen = v.length();

		if (v.start == v.data.startUsed && 1 <= v.start) {
			v.data.insertBefore(n);
			return new IVector<>(v.data, v.start - 1, v.end);
		} else {
			Data<T> data = new Data<>(vlen + 16, 0);
			data.insertAfter(n);
			data.insertAfter(v.data.nodes, v.start, v.end);
			return new IVector<>(data, data.startUsed, data.endUsed);
		}
	}

	public IVector<T> concat(IVector<T> u, IVector<T> v) {
		int ulen = u.length(), vlen = v.length();

		if (u.end == u.data.endUsed && vlen <= u.data.nodes.length - u.end) {
			u.data.insertAfter(v.data.nodes, v.start, v.end);
			return new IVector<>(u.data, u.start, u.end + vlen);
		} else if (v.start == v.data.startUsed && ulen <= v.start) {
			v.data.insertBefore(u.data.nodes, u.start, u.end);
			return new IVector<>(v.data, v.start - ulen, v.end);
		} else {
			Data<T> data = new Data<>(ulen + vlen + 16, 0);
			data.insertAfter(u.data.nodes, u.start, u.end);
			data.insertAfter(v.data.nodes, v.start, v.end);
			return new IVector<>(data, data.startUsed, data.endUsed);
		}
	}

	public T get(int i) {
		return data.nodes[start + i];
	}

	public IVector<T> range(int s, int e) {
		int length = length();
		while (s < 0)
			s += length;
		while (e <= 0)
			e += length;
		e = Math.min(e, length);
		return new IVector<>(data, start + s, start + e);
	}

	public int length() {
		return end - start;
	}

	@Override
	public boolean equals(Object object) {
		boolean b = false;

		if (Object_.clazz(object) == IVector.class) {
			@SuppressWarnings("unchecked")
			IVector<T> v = (IVector<T>) object;
			b = end - start == v.end - v.start;
			int si = start, di = v.start;

			while (b && si < end)
				b &= Objects.equals(data.nodes[si++], v.data.nodes[di++]);
		}

		return b;
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		for (int i = start; i < end; i++) {
			int h = Objects.hashCode(data.nodes[i]);
			hashCode = hashCode * 31 + h;
		}
		return hashCode;
	}

}
