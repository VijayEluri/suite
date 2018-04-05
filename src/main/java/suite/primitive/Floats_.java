package suite.primitive;

import java.io.IOException;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Floats.WriteChar;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Floats_ {

	private static int bufferSize = 65536;

	public static Outlet<Floats> buffer(Outlet<Floats> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static FltStreamlet concat(FltStreamlet... streamlets) {
		return new FltStreamlet(() -> {
			Source<FltStreamlet> source = Read.from(streamlets).outlet().source();
			return FltOutlet.of(FltFunUtil.concat(FunUtil.map(FltStreamlet::source, source)));
		});
	}

	public static float[] concat(float[]... array) {
		var length = 0;
		for (float[] fs : array)
			length += fs.length;
		float[] fs1 = new float[length];
		var i = 0;
		for (float[] fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Floats concat(Floats... array) {
		var length = 0;
		for (Floats floats : array)
			length += floats.size();
		float[] cs1 = new float[length];
		var i = 0;
		for (Floats floats : array) {
			var size_ = floats.size();
			copy(floats.cs, floats.start, cs1, i, size_);
			i += size_;
		}
		return Floats.of(cs1);
	}

	public static void copy(float[] from, int fromIndex, float[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Floats> outlet, WriteChar writer) {
		Floats floats;
		while ((floats = outlet.next()) != null)
			try {
				writer.write(floats.cs, floats.start, floats.end - floats.start);
			} catch (IOException ex) {
				Fail.t(ex);
			}
	}

	public static FltStreamlet of(float... ts) {
		return new FltStreamlet(() -> FltOutlet.of(ts));
	}

	public static FltStreamlet of(float[] ts, int start, int end, int inc) {
		return new FltStreamlet(() -> FltOutlet.of(ts, start, end, inc));
	}

	public static FltStreamlet range(float e) {
		return range((float) 0, e);
	}

	public static FltStreamlet range(float s, float e) {
		return new FltStreamlet(() -> {
			FltMutable m = FltMutable.of(s);
			return FltOutlet.of(() -> {
				var c = m.increment();
				return c < e ? c : FltFunUtil.EMPTYVALUE;
			});
		});
	}

	public static FltStreamlet reverse(float[] ts, int start, int end) {
		return new FltStreamlet(() -> FltOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Fun<Outlet<Floats>, Outlet<Floats>> split(Floats delim) {
		var ds = delim.size();

		return outlet -> Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				var size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
						p0++;
					else
						return true;
				if (!cont) {
					p0 = p1 = buffer.size();
					return true;
				} else
					return false;
			}
		});
	}

	public static float[] toArray(int length, Int_Flt f) {
		float[] cs = new float[length];
		for (int i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Floats> {
		protected Outlet<Floats> outlet;
		protected Floats buffer = Floats.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Floats> outlet) {
			this.outlet = outlet;
		}

		public Floats source() {
			Floats in;
			FloatsBuilder cb = new FloatsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toFloats();
			}

			if (cont && 0 < p0) {
				Floats head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
