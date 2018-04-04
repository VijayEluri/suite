package suite.math.linalg;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.primitive.Floats_;

public class Eigen {

	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	// Paul Wilmott on Quantitative Finance, Second Edition
	// 37.13.1 The Power Method, page 620
	public float[][] power(float[][] m0) {
		var m = mtx.of(m0);
		var size = mtx.sqSize(m);
		var eigenVectors = new float[size][];
		var eigenValue = 0f;

		for (int v = 0; v < size; v++) {
			float[] xs = Floats_.toArray(size, i -> random.nextFloat());

			for (int iteration = 0; iteration < 256; iteration++) {
				float[] ys = mtx.mul(m, xs);
				eigenValue = 0f;
				for (float y : ys)
					if (Math.abs(eigenValue) < Math.abs(y))
						eigenValue = y;
				xs = vec.scale(ys, 1d / eigenValue);
			}

			eigenVectors[v] = vec.normalize(xs);

			for (int i = 0; i < size; i++)
				m[i][i] -= eigenValue;
		}

		return eigenVectors;
	}

	// https://en.wikipedia.org/wiki/Lanczos_algorithm
	// returns V and T, where m ~= V T V*
	public Pair<float[][], float[][]> lanczos(float[][] m) {
		var n = mtx.sqSize(m);
		var nIterations = 20; // n
		var alphas = new float[nIterations];
		var betas = new float[nIterations];
		var vs = new float[nIterations][];
		var ws = new float[nIterations][];
		float[] vj1 = null;

		for (int j = 1; j < nIterations; j++) {
			var beta = 0d;
			float[] prevw;
			float[] vj;

			if (0 < j && (beta = vec.dot(prevw = ws[j - 1])) != 0d)
				vj = vec.scale(prevw, 1d / (betas[j] = (float) beta));
			else
				vj = vec.normalize(Floats_.toArray(n, i -> random.nextFloat()));

			float[] wp = mtx.mul(m, vs[j] = vj);
			float[] sub0 = vec.scale(vj, alphas[0] = (float) vec.dot(wp, vj));
			float[] sub1 = 0 < j ? vec.add(sub0, vec.scale(vj1, beta)) : sub0;

			vj1 = vj;
			ws[j] = vec.sub(wp, sub1);
		}

		var t = new float[nIterations][nIterations];

		for (int i = 0; i < nIterations; i++)
			t[i][i] = alphas[i];
		for (int i = 1; i < nIterations; i++)
			t[i - 1][i] = t[i][i - 1] = betas[i];

		return Pair.of(mtx.transpose(vs), t);
	}

	public float[] values(float[][] m, float[][] vs) {
		return Floats_.toArray(vs.length, i -> {
			var v = vs[i];
			return (float) (vec.abs(mtx.mul(m, v)) / vec.abs(v));
		});
	}

}
