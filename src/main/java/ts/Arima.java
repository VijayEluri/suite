package ts;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.Arrays;
import java.util.Random;

import suite.math.linalg.Vector;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.Floats;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.To;

public class Arima {

	private Mle mle = new Mle();
	private Statistic stat = new Statistic();
	private Random random = new Random();
	private TimeSeries ts = new TimeSeries();
	private Vector vec = new Vector();

	public LinearRegression ar(float[] ys, int n) {
		return stat.linearRegression(Ints_ //
				.range(n, ys.length) //
				.map(i -> FltObjPair.of(ys[i], Arrays.copyOfRange(ys, i - n, i))));
	}

	// Digital Processing of Random Signals, Boaz Porat, page 159
	private float[] arLevinsonDurbin(float[] ys, int p) {
		var mean = stat.meanVariance(ys).mean;
		var mean2 = mean * mean;
		var length = ys.length;

		var r = Ints_ //
				.range(p + 1) //
				.collect(Int_Flt.lift(i -> {
					double sum = Ints_.range(i, length).toDouble(Int_Dbl.sum(j -> ys[j - i] * ys[j]));
					return (float) (sum - mean2);
				})) //
				.toArray();

		double d = r[0];
		var alpha = new float[p];
		alpha[0] = 1;

		for (int n = 0; n < p; n++) {
			var alpha0 = alpha;
			var n_ = n;

			var k1 = Ints_.range(n).toDouble(Int_Dbl.sum(k -> alpha0[k] * r[n_ + 1 - k])) / d;
			d = d * (1d - k1 * k1);

			var alpha1 = new float[p];
			alpha1[0] = 1f;
			for (int k = 1; k <= n; k++)
				alpha1[k] = (float) (alpha0[k] - k1 * alpha0[n + 1 - k]);
			alpha1[n + 1] = (float) -k1;
			alpha = alpha1;
		}

		// for n in 0 until p
		// K[n + 1] := d[n] ^ -1 * summation(0 <= k < n, alpha[n][k] * r[n + 1 - k])
		// d[n + 1] := d[n] * (1 - K[n + 1] ^ 2)
		// alpha[n + 1][0] := 1
		// for k := 1 to n
		// alpha[n + 1][k] := alpha[n][k] - K[n + 1] * alpha[n][n + 1 - k]
		// alpha[n + 1][n + 1] := -K[n + 1]

		return alpha;
	}

	public DblObjPair<Arima_> arimaBackcast(float[] xs0, int p, int d, int q) {
		float[] ars = To.vector(p, i -> 1f);
		float[] mas = To.vector(q, i -> 1f);
		float[] xs1 = nDiffs(xs0, d);
		Arima_ arima = armaBackcast(xs1, ars, mas);
		float[] xs2 = Floats_.concat(xs1, new float[] { arima.x1, });
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// http://math.unice.fr/~frapetti/CorsoP/Chapitre_4_IMEA_1.pdf
	// "Least squares estimation using backcasting procedure"
	public Arima_ armaBackcast(float[] xs, float[] ars, float[] mas) {
		var length = xs.length;
		var p = ars.length;
		var q = mas.length;
		float[] xsp = Floats_.concat(new float[p], xs);
		var epq = new float[length + q];
		Arma arma = new Arma(ars, mas);

		for (int iter = 0; iter < 64; iter++) {

			// backcast
			// ep[t]
			// = (xs[t + q] - ep[t + q]
			// - ars[0] * xs[t + q - 1] - ... - ars[p - 1] * xs[t + q - p]
			// - mas[0] * ep[t + q - 1] - ... - mas[q - 2] * ep[t + 1]
			// ) / mas[q - 1]
			arma.backcast(xsp, epq);

			// forward recursion
			// ep[t] = xs[t]
			// - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
			// - mas[0] * ep[t - 1] - ... - mas[q - 1] * ep[t - q]
			double error = arma.forwardRecursion(xsp, epq);

			// minimization
			// xs[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
			// + ep[t]
			LinearRegression lr = stat.linearRegression(Ints_ //
					.range(length) //
					.map(t -> {
						int tp = t + p, tpm1 = tp - 1;
						int tq = t + q, tqm1 = tq - 1;
						FltStreamlet lrxs0 = Ints_.range(p).collect(Int_Flt.lift(i -> xsp[tpm1 - i]));
						FltStreamlet lrxs1 = Ints_.range(q).collect(Int_Flt.lift(i -> epq[tqm1 - i]));
						return FltObjPair.of(xsp[tp], Floats_.concat(lrxs0, lrxs1).toArray());
					}));

			System.out.println("iter " + iter + ", error = " + To.string(error) + lr);
			System.out.println();

			var coefficients = lr.coefficients();
			Floats_.copy(coefficients, 0, ars, 0, p);
			Floats_.copy(coefficients, p, mas, 0, q);
		}

		double x1 = arma.sum(xsp, epq);
		return new Arima_(ars, mas, (float) x1);
	}

	@SuppressWarnings("unused")
	private DblObjPair<Arima_> arimaEm(float[] xs0, int p, int d, int q) {
		float[] xs1 = nDiffs(xs0, d);
		Arima_ arima = armaEm(xs1, p, q);
		float[] xs2 = Floats_.concat(xs1, new float[] { arima.x1, });
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// xs[t] - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
	// = ep[t] + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
	private Arima_ armaEm(float[] xs, int p, int q) { // ARMA
		var length = xs.length;
		var lengthp = length + p;
		var lengthq = length + q;
		float[] ars = To.vector(p, i -> Math.scalb(.5d, -i));
		float[] mas = To.vector(q, i -> Math.scalb(.5d, -i));
		var xsp = new float[lengthp];
		float[] epq = To.vector(lengthq, i -> xs[max(0, min(xsp.length, i - q))] * .25f);

		Arrays.fill(xsp, 0, p, xs[0]);
		System.arraycopy(xs, 0, xsp, p, length);

		for (int iter = 0; iter < 9; iter++) {

			// xs[t] - ep[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
			{
				var coeffs = stat.linearRegression(Ints_ //
						.range(length) //
						.map(t -> {
							var tp = t + p;
							var tq = t + q;
							var lrxs = Floats_ //
									.concat(Floats_.reverse(xsp, t, tp), Floats_.reverse(epq, t, tq)) //
									.toArray();
							var lry = xsp[tp] - epq[tq];
							return FltObjPair.of(lry, lrxs);
						})).coefficients();

				Floats_.copy(coeffs, 0, ars, 0, p);
				Floats_.copy(coeffs, p, mas, p, q);
			}

			{
				// xs[t] - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
				// = ep[t] + ep[t - 1] * mas[0] + ... + ep[t - q] * mas[q - 1]
				var epq1 = stat.linearRegression(Ints_ //
						.range(length) //
						.map(t -> {
							var lrxs = new float[lengthq];
							var tp = t + p;
							var tq = t + q;
							lrxs[tq--] = 1f;
							for (int i = 0; i < q; i++)
								lrxs[tq--] = mas[i];
							double lry = xsp[tp] - vec.convolute(p, ars, xsp, tp);
							return FltObjPair.of((float) lry, lrxs);
						})).coefficients();

				Floats_.copy(epq1, 0, epq, 0, lengthq);
			}
		}

		// xs[t]
		// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
		// + ep[t]
		// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
		// when t = xs.length
		double x1 = new Arma(ars, mas).sum(xsp, epq);

		return new Arima_(ars, mas, (float) x1);
	}

	@SuppressWarnings("unused")
	private DblObjPair<Arima_> arimaIa(float[] xs0, int p, int d, int q) {
		float[] xs1 = nDiffs(xs0, d);
		Arima_ arima = armaIa(xs1, p, q);
		float[] xs2 = Floats_.concat(xs1, new float[] { arima.x1, });
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// extended from
	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// xs[t]
	// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
	// + ep[t]
	// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
	private Arima_ armaIa(float[] xs, int p, int q) {
		var length = xs.length;
		int lengthp = length + p, lengthpm1 = lengthp - 1;
		int lengthq = length + q, lengthqm1 = lengthq - 1;
		var iter = 0;
		var xsp = new float[lengthp];
		float[][] epqByIter = new float[q][];

		Arrays.fill(xsp, 0, p, xs[0]);
		System.arraycopy(xs, 0, xsp, p, length);

		while (true) {
			var iter_ = iter;

			LinearRegression lr = stat.linearRegression(Ints_ //
					.range(length) //
					.map(t -> {
						var tp = t + p;
						int tq = t + q, tqm1 = tq - 1;
						var lrxs = Floats_ //
								.concat(Floats_.reverse(xsp, t, tp),
										Ints_.range(iter_).collect(Int_Flt.lift(i -> epqByIter[i][tqm1 - i]))) //
								.toArray();
						return FltObjPair.of(xsp[tp], lrxs);
					}));

			var coeffs = lr.coefficients();

			if (iter < q)
				System.arraycopy(lr.residuals, 0, epqByIter[iter++] = new float[lengthq], q, length);
			else {
				float[] ars = Floats.of(coeffs, 0, p).toArray();
				float[] mas = Floats.of(coeffs, p).toArray();

				var x1 = 0d //
						+ Ints_.range(p).toDouble(Int_Dbl.sum(i -> ars[i] * xsp[lengthpm1 - i])) //
						+ Ints_.range(q).toDouble(Int_Dbl.sum(i -> mas[i] * epqByIter[i][lengthqm1 - i]));

				return new Arima_(ars, mas, (float) x1);
			}
		}
	}

	public DblObjPair<Arima_> arimaMle(float[] xs0, int p, int d, int q) {
		float[] xs1 = nDiffs(xs0, d);
		Arima_ arima = armaMle(xs1, p, q);
		float[] xs2 = Floats_.concat(xs1, new float[] { arima.x1, });
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	private Arima_ armaMle(float[] xs, int p, int q) {
		var length = xs.length;
		float[] xsp = Floats_.concat(new float[p], xs);
		var epq = new float[length + q];

		class LogLikelihood implements DblSource {
			private float[] ars = To.vector(p, i -> random.nextGaussian());
			private float[] mas = To.vector(q, i -> random.nextGaussian());
			private Arma arma = new Arma(ars, mas);

			public double source() {
				arma.backcast(xsp, epq);
				return -arma.forwardRecursion(xsp, epq);
			}
		}

		LogLikelihood ll = mle.max(LogLikelihood::new);
		var ars = ll.ars;
		var mas = ll.mas;

		double x1 = ll.arma.sum(xsp, epq);
		return new Arima_(ars, mas, (float) x1);
	}

	// Digital Processing of Random Signals, Boaz Porat, page 190
	// q << l << fs.length
	@SuppressWarnings("unused")
	private float[] maDurbin(float[] ys, int q, int l) {
		float[] ar = arLevinsonDurbin(ys, l);
		return arLevinsonDurbin(ar, q);
	}

	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// xs[t]
	// = mas[0] * 1 + mas[1] * ep[t - 1] + ... + mas[q] * ep[t - q]
	// + ep[t]
	@SuppressWarnings("unused")
	private float[] maIa(float[] xs, int q) {
		var length = xs.length;
		float[][] epqByIter = new float[q][];
		var iter = 0;
		var qm1 = q - 1;

		while (true) {
			var iter_ = iter;

			LinearRegression lr = stat.linearRegression(Ints_ //
					.range(length) //
					.map(t -> {
						var tqm1 = t + qm1;
						var lrxs = Floats_
								.concat(Floats_.of(1f), Ints_.range(iter_).collect(Int_Flt.lift(i -> epqByIter[i][tqm1 - i])))
								.toArray();
						return FltObjPair.of(xs[t], lrxs);
					}));

			if (iter < q)
				System.arraycopy(lr.residuals, 0, epqByIter[iter++] = new float[q + length], q, length);
			else
				return lr.coefficients();
		}
	}

	public class Arima_ {
		public final float[] ars;
		public final float[] mas;
		public final float x1;

		private Arima_(float[] ars, float[] mas, float x1) {
			this.ars = ars;
			this.mas = mas;
			this.x1 = x1;
		}
	}

	private class Arma {
		private int p, q;
		private float[] ars;
		private float[] mas;

		private Arma(float[] ars, float[] mas) {
			p = ars.length;
			q = mas.length;
			this.ars = ars;
			this.mas = mas;
		}

		private void backcast(float[] xsp, float[] epq) {
			var qm1 = q - 1;

			for (int t = qm1; 0 <= t; t--) {
				double sum = sum(xsp, epq, t, p, qm1);
				epq[t] = (float) ((xsp[t + p] - epq[t + q] - sum) / mas[qm1]);
			}
		}

		private double forwardRecursion(float[] xsp, float[] epq) {
			var length = xsp.length - p;
			var error = 0d;

			for (int t = 0; t < length; t++) {
				var tp = t + p;
				var tq = t + q;
				double ep = xsp[tp] - sum(xsp, epq, t, p, q);
				epq[tq] = (float) ep;
				error += ep * ep;
			}

			return error;
		}

		private double sum(float[] xsp, float[] epq) {
			return sum(xsp, epq, xsp.length - p, p, q);
		}

		private double sum(float[] xsp, float[] epq, int t, int p_, int q_) {
			return vec.convolute(p_, ars, xsp, t + p) + vec.convolute(q_, mas, epq, t + q);
		}
	}

	private float[] nDiffs(float[] xs, int d) {
		for (int i = 0; i < d; i++)
			xs = ts.dropDiff(1, xs);
		return xs;
	}

	private float nSums(float[] xs, int d) {
		var lengthm1 = xs.length - 1;
		for (int i = 0; i < d; i++) {
			var l = lengthm1;
			for (int j = i; j < d; j++) {
				var l0 = l;
				xs[l0] += xs[--l];
			}
		}
		return xs[lengthm1];
	}

}
