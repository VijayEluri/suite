package suite.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import suite.Constants;
import suite.math.Vector;
import suite.os.LogUtil;
import suite.primitive.Ints_;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil2.BiFun;
import suite.util.Thread_;

public class Render {

	public static BufferedImage render(int width, int height, BiFun<Float, Vector> f) {
		int nThreads = Constants.nThreads;
		int[] xs = Ints_.toArray(nThreads + 1, i -> width * i / nThreads);

		Vector pixels[][] = new Vector[width][height];
		float scale = 1f / Math.max(width, height);
		int centreX = width / 2, centreY = height / 2;

		List<Thread> threads = IntStreamlet //
				.range(nThreads) //
				.map(t -> Thread_.newThread(() -> {
					for (int x = xs[t]; x < xs[t + 1]; x++)
						for (int y = 0; y < height; y++) {
							Vector color;
							try {
								color = f.apply((x - centreX) * scale, (y - centreY) * scale);
							} catch (Exception ex) {
								LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
								color = new Vector(1f, 1f, 1f);
							}
							pixels[x][y] = color;
						}
				})) //
				.toList();

		Thread_.startJoin(threads);

		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Vector pixel = limit(pixels[x][y]);
				bufferedImage.setRGB(x, y, new Color(pixel.x, pixel.y, pixel.z).getRGB());
			}

		return bufferedImage;
	}

	private static Vector limit(Vector u) {
		return new Vector(limit(u.x), limit(u.y), limit(u.z));
	}

	private static float limit(float f) {
		return Math.min(1f, Math.max(0f, f));
	}

}
