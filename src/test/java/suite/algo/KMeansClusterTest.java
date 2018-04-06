package suite.algo;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class KMeansClusterTest {

	private Random random = new Random();

	@Test
	public void test() {
		var n = 3;

		Map<String, Source<float[]>> seeds = Map.ofEntries( //
				entry("A", () -> point(-16f, 16f, 16f)), //
				entry("B", () -> point(16f, -16f, 16f)), //
				entry("C", () -> point(16f, 16f, -16f)));

		var points = Read //
				.from2(seeds) //
				.concatMap2((prefix, source) -> Ints_ //
						.range(n) //
						.map2(i -> prefix + i, i -> source.source())) //
				.toMap();

		var kmc = new KmeansCluster(seeds.size());
		var clusters = kmc.kMeansCluster(points, n, 9);

		assertEquals(9, clusters.size());

		for (var prefix : seeds.keySet())
			for (var i : Ints_.range(n))
				assertEquals(clusters.get(prefix + "0"), clusters.get(prefix + i));
	}

	private float[] point(float x, float y, float z) {
		return new float[] { //
				(float) (x + random.nextGaussian()), //
				(float) (y + random.nextGaussian()), //
				(float) (z + random.nextGaussian()), //
		};
	}

}
