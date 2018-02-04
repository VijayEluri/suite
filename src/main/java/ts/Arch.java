package ts;

import java.util.Arrays;
import java.util.Random;

import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.To;

public class Arch {

	private Mle mle = new Mle();
	private Statistic stat = new Statistic();
	private Random random = new Random();

	public float[] arch(float[] ys, int p, int q) {

		// auto regressive
		int length = ys.length;
		float[][] xs0 = To.array(length, float[].class, i -> copyPadZeroes(ys, i - p, i));
		LinearRegression lr0 = stat.linearRegression(xs0, ys, null);
		float[] variances = To.vector(lr0.residuals, residual -> residual * residual);

		// conditional heteroskedasticity
		LinearRegression lr1 = stat.linearRegression(Ints_ //
				.range(length) //
				.map(i -> FltObjPair.of(variances[i], copyPadZeroes(variances, i - p, i))));

		return Floats_.concat(lr0.coefficients, lr1.coefficients);
	}

	// https://quant.stackexchange.com/questions/9351/algorithm-to-fit-ar1-garch1-1-model-of-log-returns
	public Object[] garchp1(float[] xs, int p) {
		class LogLikelihood implements DblSource {
			private double c = random.nextDouble() * .0001d;
			private float[] ars = To.vector(p, i -> random.nextDouble() * .01d);
			private double p0 = random.nextDouble() * .00002d;
			private double p1 = random.nextDouble() * .001d;
			private double p2 = .9d + random.nextDouble() * .001d;

			public double source() {
				double eps = 0d;
				double var = 0d;
				double logLikelihood = 0d;

				for (int t = p; t < xs.length; t++) {
					int tm1 = t - 1;
					double eps0 = eps;
					double var0 = var;
					double estx = c + Ints_.range(p).toDouble(Int_Dbl.sum(i -> ars[i] * xs[tm1 - i]));
					eps = xs[t] - estx;
					var = p0 + p1 * eps0 * eps0 + p2 * var0;
					logLikelihood += -.5d * (Math.log(var) + eps * eps / var);
				}

				return logLikelihood;
			}
		}

		LogLikelihood ll = mle.max(LogLikelihood::new);
		return new Object[] { ll.c, ll.ars, ll.p0, ll.p1, ll.p2, };
	}

	private float[] copyPadZeroes(float[] fs0, int from, int to) {
		float[] fs1 = new float[to - from];
		int p = -Math.max(0, from);
		Arrays.fill(fs1, 0, p, 0f);
		Floats_.copy(fs0, 0, fs1, p, to - p);
		return fs1;
	}

}