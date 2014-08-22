package suite.primitive;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.util.Copy;
import suite.util.To;
import suite.util.Util;

public class Chars implements Iterable<Character> {

	private char cs[]; // Immutable
	private int start, end;

	private static char emptyCharArray[] = new char[0];
	private static int reallocSize = 65536;

	public static Chars emptyChars = new Chars(emptyCharArray);

	public static Comparator<Chars> comparator = (chars0, chars1) -> {
		int start0 = chars0.start, start1 = chars1.start;
		int size0 = chars0.size(), size1 = chars1.size(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			char c0 = chars0.cs[start0 + index];
			char c1 = chars1.cs[start1 + index];
			c = c0 == c1 ? 0 : c0 > c1 ? 1 : -1;
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public Chars(String s) {
		this(To.charArray(s));
	}

	public Chars(Chars chars) {
		this(chars.cs, chars.start, chars.end);
	}

	public Chars(char chars[]) {
		this(chars, 0);
	}

	public Chars(char chars[], int start) {
		this(chars, start, chars.length);
	}

	public Chars(char chars[], int start, int end) {
		this.cs = chars;
		this.start = start;
		this.end = end;
	}

	public Chars append(Chars a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		char nb[] = new char[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return new Chars(nb);
	}

	public static Chars asList(char... in) {
		return new Chars(in);
	}

	public static Chars concat(Chars... array) {
		CharsBuilder bb = new CharsBuilder();
		for (Chars chars : array)
			bb.append(chars);
		return bb.toChars();
	}

	public char get(int index) {
		if (index < 0)
			index += size();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public boolean isEmpty() {
		return start >= end;
	}

	public Chars pad(int size, char pad) {
		CharsBuilder cb = new CharsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append(pad);
		return cb.toChars();
	}

	public int size() {
		return end - start;
	}

	public Chars subchars(int s) {
		return subchars0(start + s, end);
	}

	public Chars subchars(int s, int e) {
		if (s < 0)
			s += size();
		if (e < s)
			e += size();

		return subchars0(start + s, start + e);
	}

	@Override
	public Iterator<Character> iterator() {
		return new Iterator<Character>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Character next() {
				return cs[pos++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++)
			result = 31 * result + cs[i];
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Chars.class) {
			Chars other = (Chars) object;

			if (end - start == other.end - other.start) {
				int diff = other.start - start;
				for (int i = start; i < end; i++)
					if (cs[i] != other.cs[i + diff])
						return false;
				return true;
			} else
				return false;
		} else
			return false;
	}

	@Override
	public String toString() {
		return new String(cs, start, end - start);
	}

	private Chars subchars0(int start, int end) {
		checkOpenBounds(start);
		checkOpenBounds(end);
		Chars result = new Chars(cs, start, end);

		// Avoid small pack of chars object keeping a large buffer
		if (cs.length >= reallocSize && end - start < reallocSize / 4)
			result = emptyChars.append(result); // Do not share reference

		return result;
	}

	private void checkOpenBounds(int index) {
		if (index < start || index > end)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "}");
	}

	private void checkClosedBounds(int index) {
		if (index < start || index >= end)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "]");
	}

	public char[] getChars() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public static class CharsBuilder {
		private char chars[] = emptyCharArray;
		private int size;

		public CharsBuilder append(Chars b) {
			return append(b.cs, b.start, b.end);
		}

		public CharsBuilder append(char b) {
			extendBuffer(size + 1);
			chars[size++] = b;
			return this;
		}

		public CharsBuilder append(char b[]) {
			return append(b, 0, b.length);
		}

		public CharsBuilder append(char b[], int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.primitiveArray(b, start, chars, size, inc);
			size += inc;
			return this;
		}

		public void clear() {
			size = 0;
		}

		public void extend(int size1) {
			extendBuffer(size1);
			size = size1;
		}

		public int size() {
			return size;
		}

		public Chars toChars() {
			return new Chars(chars, 0, size);
		}

		private void extendBuffer(int capacity1) {
			int capacity0 = chars.length;

			if (capacity0 < capacity1) {
				int capacity = Math.max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < 4096 ? capacity << 1 : capacity * 3 / 2;

				char chars1[] = new char[capacity];
				Copy.primitiveArray(chars, 0, chars1, 0, size);
				chars = chars1;
			}
		}
	}

}
