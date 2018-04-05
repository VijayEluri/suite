package suite.math.numeric;

import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;

public class ConjugateGradient {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	// https://en.wikipedia.org/wiki/Conjugate_gradient_method
	public float[] linear(float[][] a, float[] b, float[] initials) {
		var xs = initials;
		float[] rs = vec.sub(b, mtx.mul(a, xs));
		var ps = rs;

		for (var iter = 0; iter < initials.length; iter++) {
			double alpha = vec.dot(rs) / vec.dot(ps, mtx.mul(a, ps));
			float[] xs1 = vec.add(xs, vec.scale(ps, alpha));
			float[] rs1 = vec.sub(rs, vec.scale(mtx.mul(a, ps), alpha));
			var beta = vec.dot(rs1) / vec.dot(rs);
			float[] ps1 = vec.add(rs1, vec.scale(ps, beta));

			xs = xs1;
			rs = rs1;
			ps = ps1;
		}

		return xs;
	}

}
