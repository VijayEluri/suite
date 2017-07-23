package suite.math;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.primitive.Floats_;
import suite.streamlet.Read;

public class Eigen {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public float[][] power(float[][] m0) {
		float[][] m = mtx.of(m0);
		for (int i = 0; i < 20; i++) {
			m = Read.from(m).map(mtx::normalize).toArray(float[].class);
			m = mtx.mul(m, m);
			m = mtx.mul(m, m0);
		}
		return m;
	}

	// Paul Wilmott on Quantitative Finance, Second Edition
	// 37.13.1 The Power Method, page 620
	public float[][] powerPca(float[][] m0) {
		float[][] m = m0;
		int size = mtx.sqSize(m);
		float[][] eigenVectors = new float[size][];
		float[] xs = Floats_.toArray(size, i -> random.nextFloat());
		float eigenValue = Float.MIN_VALUE;

		for (int v = 0; v < size; v++) {
			for (int iteration = 0; iteration < 20; iteration++) {
				float[] ys = mtx.mul(m, xs);
				eigenValue = Float.MIN_VALUE;
				for (float y : ys)
					eigenValue = Math.max(eigenValue, y);
				xs = mtx.scale(ys, 1d / eigenValue);
			}

			float[] eigenVector = eigenVectors[v] = mtx.normalize(xs);

			for (int i = 0; i < size; i++)
				for (int j = 0; j < size; j++)
					m[i][j] -= eigenValue * eigenVector[i] * eigenVector[j];
		}

		return eigenVectors;
	}

	// https://en.wikipedia.org/wiki/Lanczos_algorithm
	// returns V and T, where m ~= V T V*
	public Pair<float[][], float[][]> lanczos(float[][] m) {
		int n = mtx.sqSize(m);
		int nIterations = 20; // n
		float[] alphas = new float[nIterations];
		float[] betas = new float[nIterations];
		float[][] vs = new float[nIterations][];
		float[][] ws = new float[nIterations][];
		float[] vj1 = null;

		for (int j = 1; j < nIterations; j++) {
			float beta = 0f;
			float[] prevw;
			float[] vj;

			if (0 < j && (beta = mtx.dot(prevw = ws[j - 1])) != 0d)
				vj = mtx.scale(prevw, 1d / (betas[j] = beta));
			else
				vj = mtx.normalize(Floats_.toArray(n, i -> random.nextFloat()));

			float[] wp = mtx.mul(m, vs[j] = vj);
			float[] sub0 = mtx.scale(vj, alphas[0] = mtx.dot(wp, vj));
			float[] sub1 = 0 < j ? mtx.add(sub0, mtx.scale(vj1, beta)) : sub0;

			vj1 = vj;
			ws[j] = mtx.sub(wp, sub1);
		}

		float[][] t = new float[nIterations][nIterations];

		for (int i = 0; i < nIterations; i++)
			t[i][i] = alphas[i];
		for (int i = 1; i < nIterations; i++)
			t[i - 1][i] = t[i][i - 1] = betas[i];

		return Pair.of(mtx.transpose(vs), t);
	}

	public float[] values(float[][] m, float[][] vs) {
		return Floats_.toArray(vs.length, i -> {
			float[] v = vs[i];
			return (float) (mtx.abs(mtx.mul(m, v)) / mtx.abs(v));
		});
	}

}
