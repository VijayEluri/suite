package suite.file;

import java.io.Closeable;

import suite.util.Object_;

public interface ExtentAllocator extends Closeable {

	public class Extent {
		public final int start;
		public final int end;

		public Extent(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public String toString() {
			return start + "-" + end;
		}

		public boolean equals(Object object) {
			if (Object_.clazz(object) == Extent.class) {
				Extent other = (Extent) object;
				return start == other.start && end == other.end;
			} else
				return false;
		}

		public int hashCode() {
			return start ^ end;
		}
	}

	public void create();

	public Extent allocate(int count);

	public void deallocate(Extent extent);

}
