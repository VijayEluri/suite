package suite.primitive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.util.Copy;
import suite.util.To;
import suite.util.Util;

public class Bytes implements Iterable<Byte> {

	private byte bs[]; // Immutable
	private int start, end;

	private static byte emptyByteArray[] = new byte[0];
	private static int reallocSize = 65536;

	public static Bytes emptyBytes = Bytes.of(emptyByteArray);

	public static Comparator<Bytes> comparator = (bytes0, bytes1) -> {
		int start0 = bytes0.start, start1 = bytes1.start;
		int size0 = bytes0.size(), size1 = bytes1.size(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			byte b0 = bytes0.bs[start0 + index];
			byte b1 = bytes1.bs[start1 + index];
			c = b0 == b1 ? 0 : b0 > b1 ? 1 : -1;
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	private Bytes(byte bytes[], int start, int end) {
		this.bs = bytes;
		this.start = start;
		this.end = end;
	}

	public Bytes append(Bytes a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		byte nb[] = new byte[newSize];
		System.arraycopy(bs, start, nb, 0, size0);
		System.arraycopy(a.bs, a.start, nb, size0, size1);
		return Bytes.of(nb);
	}

	public static Bytes asList(byte... in) {
		return Bytes.of(in);
	}

	public static Bytes concat(Bytes... array) {
		BytesBuilder bb = new BytesBuilder();
		for (Bytes bytes : array)
			bb.append(bytes);
		return bb.toBytes();
	}

	public DataInput dataInput() {
		return new DataInputStream(new ByteArrayInputStream(bs, start, end - start));
	}

	public byte get(int index) {
		if (index < 0)
			index += size();
		int i1 = index + start;
		checkClosedBounds(i1);
		return bs[i1];
	}

	public boolean isEmpty() {
		return start >= end;
	}

	public static Bytes of(IoSink<DataOutput> ioSink) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ioSink.sink(new DataOutputStream(baos));
		return Bytes.of(baos.toByteArray());
	}

	public static Bytes of(ByteBuffer bb) {
		int offset = bb.arrayOffset();
		return Bytes.of(bb.array(), offset, offset + bb.limit());
	}

	public static Bytes of(Bytes bytes) {
		return Bytes.of(bytes.bs, bytes.start, bytes.end);
	}

	public static Bytes of(byte bytes[]) {
		return Bytes.of(bytes, 0);
	}

	public static Bytes of(byte bytes[], int start) {
		return Bytes.of(bytes, start, bytes.length);
	}

	public static Bytes of(byte bytes[], int start, int end) {
		return new Bytes(bytes, start, end);
	}

	public Bytes pad(int size) {
		BytesBuilder bb = new BytesBuilder();
		bb.append(this);
		while (bb.size() < size)
			bb.append((byte) 0);
		return bb.toBytes();
	}

	public int size() {
		return end - start;
	}

	public Bytes subbytes(int s) {
		return subbytes0(start + s, end);
	}

	public Bytes subbytes(int s, int e) {
		if (s < 0)
			s += size();
		if (e < s)
			e += size();

		return subbytes0(start + s, start + e);
	}

	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(bs, start, end - start);
	}

	public byte[] toBytes() {
		if (start != 0 || end != bs.length)
			return Arrays.copyOfRange(bs, start, end);
		else
			return bs;
	}

	public void write(DataOutput dataOutput) throws IOException {
		dataOutput.write(bs, start, end - start);
	}

	public void write(OutputStream os) throws IOException {
		os.write(bs, start, end - start);
	}

	@Override
	public Iterator<Byte> iterator() {
		return new Iterator<Byte>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Byte next() {
				return bs[pos++];
			}
		};
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++)
			result = 31 * result + bs[i];
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Bytes.class) {
			Bytes other = (Bytes) object;

			if (end - start == other.end - other.start) {
				int diff = other.start - start;
				for (int i = start; i < end; i++)
					if (bs[i] != other.bs[i + diff])
						return false;
				return true;
			} else
				return false;
		} else
			return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(" " + To.hex2(bs[i]));
		return sb.toString();
	}

	private Bytes subbytes0(int start, int end) {
		checkOpenBounds(start);
		checkOpenBounds(end);
		Bytes result = Bytes.of(bs, start, end);

		// Avoid small pack of bytes object keeping a large buffer
		if (bs.length >= reallocSize && end - start < reallocSize / 4)
			result = emptyBytes.append(result); // Do not share reference

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

	public static class BytesBuilder {
		private byte bs[] = emptyByteArray;
		private int size;

		public BytesBuilder append(Bytes bytes) {
			return append(bytes.bs, bytes.start, bytes.end);
		}

		public BytesBuilder append(byte b) {
			extendBuffer(size + 1);
			bs[size++] = b;
			return this;
		}

		public BytesBuilder append(byte bs_[]) {
			return append(bs_, 0, bs_.length);
		}

		public BytesBuilder append(byte bs_[], int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.primitiveArray(bs_, start, bs, size, inc);
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

		public Bytes toBytes() {
			return Bytes.of(bs, 0, size);
		}

		private void extendBuffer(int capacity1) {
			int capacity0 = bs.length;

			if (capacity0 < capacity1) {
				int capacity = Math.max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < 4096 ? capacity << 1 : capacity * 3 / 2;

				byte bytes1[] = new byte[capacity];
				Copy.primitiveArray(bs, 0, bytes1, 0, size);
				bs = bytes1;
			}
		}
	}

}
