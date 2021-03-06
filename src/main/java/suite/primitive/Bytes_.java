package suite.primitive;

import static suite.util.Friends.rethrow;

import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.Bytes.WriteByte;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;

public class Bytes_ {

	private static int bufferSize = 65536;

	public static Outlet<Bytes> buffer(Outlet<Bytes> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(byte[] from, int fromIndex, byte[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Bytes> outlet, WriteByte writer) {
		rethrow(() -> {
			Bytes bytes;
			while ((bytes = outlet.next()) != null)
				writer.write(bytes.bs, bytes.start, bytes.end - bytes.start);
			return bytes;
		});
	}

	public static Iterate<Outlet<Bytes>> split(Bytes delim) {
		var ds = delim.size();

		return outlet -> Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				var size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
						p0++;
					else
						return true;
				var b = !cont;
				if (b)
					p0 = p1 = buffer.size();
				return b;
			}
		});
	}

	private static abstract class BufferedSource implements Source<Bytes> {
		protected Outlet<Bytes> outlet;
		protected Bytes buffer = Bytes.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Bytes> outlet) {
			this.outlet = outlet;
		}

		public Bytes g() {
			Bytes in;
			var bb = new BytesBuilder();
			bb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				bb.append(in);
				buffer = bb.toBytes();
			}

			if (cont && 0 < p0) {
				var head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
