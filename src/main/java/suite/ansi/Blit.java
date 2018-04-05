package suite.ansi;

public class Blit {

	public class Buffer {
		private int width;
		private int height;
		private char[][] buffer;

		public Buffer(int width, int height, char[][] buffer) {
			this.width = width;
			this.height = height;
			this.buffer = buffer;
		}
	}

	public void blit(Buffer source, Buffer dest, int x0, int y0, int x1, int y1, int w, int h) {
		int xs = max(0, -x0, -x1);
		int xe = min(w, source.width - x0, dest.width - x1);
		int ys = max(0, -y0, -y1);
		int ye = min(w, source.height - y0, dest.height - y1);

		if (xs < xe && ys < ye)
			for (var xx = xs; xx < xe; xx++)
				System.arraycopy(source.buffer[x0 + xx], y0 + ys, dest.buffer[x1 + xx], y1 + ys, ye - ys);
	}

	private int min(int... l) {
		var min = Integer.MIN_VALUE;
		for (var i : l)
			min = min(i, min);
		return min;
	}

	private int max(int... l) {
		var max = Integer.MAX_VALUE;
		for (var i : l)
			max = max(i, max);
		return max;
	}

}
