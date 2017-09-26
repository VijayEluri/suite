package suite.algo;

import java.util.List;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix;
import suite.primitive.Floats_;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;

/**
 * Radial basis function network with K-means clustering.
 *
 * @author ywsing
 */
public class RadialBasisFunctionNetwork {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Matrix mtx = new Matrix();

	private int nHiddens = 19;
	private float[][] centers;
	private float[] invVariances;

	public Fun<float[], float[]> train(float[][] ins, float[][] outs) {
		int length = ins[0].length;
		int[] kmc = new KmeansCluster(length).kMeansCluster(List.of(ins), nHiddens, nHiddens);

		int[] sizes = new int[nHiddens];
		float[][] sums = new float[nHiddens][length];
		float[] variances = new float[nHiddens];

		for (int i = 0; i < ins.length; i++) {
			int cl = kmc[i];
			sizes[cl]++;
			mtx.addOn(sums[cl], ins[i]);
		}

		centers = Ints_.range(nHiddens).map(cl -> mtx.scale(sums[cl], 1d / sizes[cl])).toArray(float[].class);

		for (int i = 0; i < ins.length; i++) {
			int cl = kmc[i];
			variances[cl] += mtx.dotDiff(ins[i], centers[cl]);
		}

		invVariances = Floats_.toArray(variances.length, i -> 1f / variances[i]);
		float[][] rbfs = Read.from(ins).map(this::evaluateRbfs).toArray(float[].class);
		float[][] rbfs_t = mtx.transpose(rbfs);
		Iterate<float[]> cdf = cd.inverseMul(mtx.mul(rbfs_t, rbfs));
		float[][] psi = Read.from(rbfs).map(cdf).toArray(float[].class);
		return in -> mtx.mul(evaluateRbfs(in), psi);
	}

	private float[] evaluateRbfs(float[] in) {
		return Ints_ //
				.range(nHiddens) //
				.collect(Int_Flt.lift(cl -> (float) Math.exp(-.5d * mtx.dotDiff(in, centers[cl]) * invVariances[cl]))) //
				.toArray();
	}

}
