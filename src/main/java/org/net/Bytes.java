package org.net;

import java.util.Arrays;

import org.util.Util;

public class Bytes {

	private byte bytes[]; // Immutable
	private int start, end;

	private static final byte emptyByteArray[] = new byte[0];

	private static final int reallocSize = 65536;

	public static final Bytes emptyBytes = new Bytes(emptyByteArray);

	public Bytes(byte bytes[]) {
		this(bytes, 0);
	}

	public Bytes(byte bytes[], int start) {
		this(bytes, start, bytes.length);
	}

	public Bytes(byte bytes[], int start, int end) {
		this.bytes = bytes;
		this.start = start;
		this.end = end;
	}

	public Bytes(Bytes bytes) {
		this.bytes = bytes.bytes;
		this.start = bytes.start;
		this.end = bytes.end;
	}

	public boolean isEmpty() {
		return start >= end;
	}

	public int size() {
		return end - start;
	}

	public Bytes subbytes(int start) {
		return subbytes0(this.start + start, end);
	}

	public Bytes subbytes(int start, int end) {
		return subbytes0(this.start + start, this.start + end);
	}

	private Bytes subbytes0(int start, int end) {
		checkOpenBounds(start);
		checkOpenBounds(end);
		Bytes result = new Bytes(bytes, start, end);

		// Avoids small range bytes object keeping a large buffer
		if (bytes.length >= reallocSize && end - start < reallocSize / 4)
			result = emptyBytes.append(result); // Do not share reference

		return result;
	}

	public Bytes append(Bytes a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		byte nb[] = new byte[newSize];
		System.arraycopy(bytes, start, nb, 0, size0);
		System.arraycopy(a.bytes, a.start, nb, size0, size1);
		return new Bytes(nb);
	}

	public byte byteAt(int index) {
		int i1 = index + start;
		checkClosedBounds(i1);
		return bytes[i1];
	}

	private void checkOpenBounds(int index) {
		if (index < start || index > end)
			throw new IndexOutOfBoundsException("Index " + (index - start)
					+ " is not within [0-" + (end - start) + "}");
	}

	private void checkClosedBounds(int index) {
		if (index < start || index >= end)
			throw new IndexOutOfBoundsException("Index " + (index - start)
					+ " is not within [0-" + (end - start) + "]");
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++)
			result = 31 * result + bytes[i];
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Bytes) {
			Bytes other = (Bytes) object;
			int diff = other.start - start;

			for (int i = start; i < end; i++)
				if (bytes[i] != other.bytes[i + diff])
					return false;

			return true;
		} else
			return false;
	}

	private static final String hexDigits = "0123456789ABCDEF";

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(" ") //
					.append(hexDigits.charAt((bytes[i] >>> 4) & 0x0F)) //
					.append(hexDigits.charAt(bytes[i] & 0x0F));
		return sb.toString();
	}

	public byte[] getBytes() {
		if (start != 0 || end != bytes.length)
			return Arrays.copyOfRange(bytes, start, end);
		else
			return bytes;
	}

	public static class BytesBuilder {
		private byte bytes[] = emptyByteArray;
		private int size;

		public BytesBuilder() {
		}

		public void clear() {
			size = 0;
		}

		public BytesBuilder append(Bytes b) {
			return append(b.bytes, b.start, b.end);
		}

		public BytesBuilder append(byte b) {
			return append(new byte[] { b });
		}

		public BytesBuilder append(byte b[]) {
			return append(b, 0, b.length);
		}

		public BytesBuilder append(byte b[], int start, int end) {
			int inc = end - start, size1 = size + inc;

			if (bytes.length < size1)
				extendBuffer(size1);

			Util.copyPrimitiveArray(b, start, bytes, size, inc);

			size += inc;
			return this;
		}

		public void extend(int size1) {
			extendBuffer(size1);
			size = size1;
		}

		public Bytes toBytes() {
			return new Bytes(bytes, 0, size);
		}

		public void setByteAt(int n, byte b) {
			bytes[n] = b;
		}

		public byte byteAt(int n) {
			return bytes[n];
		}

		public int getSize() {
			return size;
		}

		private void extendBuffer(int size1) {
			int length1 = bytes.length != 0 ? bytes.length : 1;
			while (length1 < size1)
				length1 = length1 < 4096 ? length1 << 1 : length1 * 3 / 2;

			byte bytes1[] = new byte[length1];
			Util.copyPrimitiveArray(bytes, 0, bytes1, 0, size);
			bytes = bytes1;
		}
	}

}
