package suite.algo;

import java.util.Random;

import org.junit.Test;

import suite.algo.Statistic.LinearRegression;
import suite.inspect.Dump;
import suite.math.MathUtil;
import suite.math.Matrix;
import suite.util.To;

public class StatisticTest {

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	@Test
	public void testCovariance() {
		float[] fs = { -.5f, 1f, };
		MathUtil.verifyEquals(.5625f, (float) stat.covariance(fs, fs), 0f);
	}

	@Test
	public void testLinearRegression() {
		int m = 7, n = 9;
		Random random = new Random();
		float[] expect = To.arrayOfFloats(m, j -> random.nextFloat());
		float[][] xs = To.arrayOfFloats(n, m, (i, j) -> random.nextFloat());
		float[] ys = To.arrayOfFloats(xs, x -> (float) (mtx.dot(expect, x) + random.nextGaussian() * .01f));
		LinearRegression lr = stat.linearRegression(xs, ys);
		Dump.out(lr);
		float[] actual = lr.betas;
		mtx.verifyEquals(expect, actual, .1f);

		float[] xtest = To.arrayOfFloats(m, j -> random.nextFloat());
		MathUtil.verifyEquals(mtx.dot(expect, xtest), lr.predict(xtest), .1f);
	}

}
