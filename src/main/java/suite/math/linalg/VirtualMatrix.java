package suite.math.linalg;

import suite.primitive.IntInt_Flt;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.util.Fail;
import suite.util.To;

public class VirtualMatrix {

	public final int height;
	public final int width_;
	public final IntInt_Flt get;

	public interface Apply<T> {
		public T apply(int height, int width_, IntInt_Flt get);
	}

	public static VirtualMatrix ofIdentity(int rank) {
		return of(rank, rank, (i, j) -> i != j ? 0f : 1f);
	}

	public static VirtualMatrix ofDiagonal(float[] fs) {
		int rank = fs.length;
		return of(rank, rank, (i, j) -> i != j ? 0f : fs[i]);
	}

	public static VirtualMatrix of(float[] fs) {
		return of(fs.length, 1, (i, j) -> fs[i]);
	}

	public static VirtualMatrix ofTranspose(float[] fs) {
		return of(1, fs.length, (i, j) -> fs[j]);
	}

	public static VirtualMatrix of(float[][] matrix) {
		Matrix_ mtx = VirtualMatrixUtil.mtx;
		return of(mtx.height(matrix), mtx.width(matrix), (i, j) -> matrix[i][j]);
	}

	public static VirtualMatrix of(int height, int width_, IntInt_Flt get) {
		return new VirtualMatrix(height, width_, get);
	}

	private VirtualMatrix(int height, int width_, IntInt_Flt get) {
		this.height = height;
		this.width_ = width_;
		this.get = get;
	}

	public VirtualMatrix add(VirtualMatrix vm1) {
		VirtualMatrix vm0 = this;
		IntInt_Flt f0 = vm0.get;
		IntInt_Flt f1 = vm1.get;
		return VirtualMatrixUtil.checkSizes(vm0, vm1, (i, j) -> f0.apply(i, j) + f1.apply(i, j));
	}

	public <T> T apply(Apply<T> apply) {
		return apply_(apply);
	}

	public VirtualMatrix buffer() {
		return of(matrix());
	}

	public VirtualMatrix convolute(float[][] k) {
		Matrix_ mtx = VirtualMatrixUtil.mtx;
		int kh = mtx.height(k);
		int kw = mtx.width(k);
		return VirtualMatrix.of( //
				height - kh + 1, //
				width_ - kw + 1, //
				(i, j) -> {
					double sum = 0d;
					for (int di = 0; di < kh; di++)
						for (int dj = 0; dj < kw; dj++)
							sum += get.apply(i + di, j + dj) * k[di][dj];
					return (float) sum;
				});
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		dump(sb);
		return sb.toString();
	}

	public void dump(StringBuilder sb) {
		sb.append("[ ");
		for (int i = 0; i < height; i++) {
			for (int j = 0; i < width_; j++)
				sb.append(To.string(get.apply(i, j)) + " ");
			sb.append("\n");
		}
	}

	public VirtualVector mul(VirtualVector vv) {
		return apply((ix, jx, f0) -> vv.apply((length, f1) -> {
			float[] o = new float[ix];
			int i1, j1;

			if (jx == length)
				for (int i0 = 0; i0 < ix; i0 = i1) {
					i1 = Math.min(i0 + 64, ix);
					for (int j0 = 0; j0 < jx; j0 = j1) {
						j1 = Math.min(j0 + 64, jx);
						for (int i = i0; i < i1; i++)
							for (int j = j0; j < j1; j++)
								o[i] += f0.apply(i, j) * f1.apply(j);
					}
				}
			else
				Fail.t("wrong input sizes");

			return VirtualVector.of(o);
		}));
	}

	public VirtualMatrix mul(VirtualMatrix vm1) {
		return apply((height, ks0, f0) -> vm1.apply((ks1, width_, f1) -> {
			float[][] o = new float[height][width_];
			int i1, j1, k1;

			if (ks0 == ks1)
				for (int i0 = 0; i0 < height; i0 = i1) {
					i1 = Math.min(i0 + 64, height);
					for (int j0 = 0; j0 < width_; j0 = j1) {
						j1 = Math.min(j0 + 64, width_);
						for (int k0 = 0; k0 < ks0; k0 = k1) {
							k1 = Math.min(k0 + 64, ks0);
							for (int i = i0; i < i1; i++)
								for (int j = j0; j < j1; j++)
									for (int k = k0; k < k1; k++)
										o[i][j] += f0.apply(i, k) * f1.apply(k, j);
						}
					}
				}
			else
				Fail.t("wrong input sizes");

			return of(o);
		}));
	}

	public VirtualVector mulLazy(VirtualVector vv) {
		return apply((height, l, f0) -> vv.apply((length, f1) -> {
			if (l == length)
				return VirtualVector.of(height,
						i -> (float) Ints_.range(l).toDouble(Int_Dbl.sum(j -> f0.apply(i, j) * f1.apply(j))));
			else
				return Fail.t("wrong input sizes");
		}));
	}

	public VirtualMatrix mulLazy(VirtualMatrix vm1) {
		return apply((height, k0s, f0) -> apply((ks1, width_, f1) -> {
			if (k0s == ks1)
				return of(height, width_,
						(i, j) -> (float) Ints_.range(k0s).toDouble(Int_Dbl.sum(k -> f0.apply(i, k) * f1.apply(k, j))));
			else
				return Fail.t("wrong input sizes");
		}));
	}

	public VirtualMatrix scale(double d) {
		return of(height, width_, (i, j) -> (float) (get.apply(i, j) * d));
	}

	public VirtualMatrix transpose() {
		return of(width_, height, (i, j) -> get.apply(j, i));
	}

	public float[][] matrix() {
		float[][] matrix = new float[height][width_];
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width_; j++)
				matrix[i][j] = get.apply(i, j);
		return matrix;
	}

	private <T> T apply_(Apply<T> apply) {
		return apply.apply(height, width_, get);
	}

}

class VirtualMatrixUtil {

	public static Matrix_ mtx = new Matrix_();

	public static VirtualMatrix checkSizes(VirtualMatrix vm0, VirtualMatrix vm1, IntInt_Flt fun) {
		return vm0.apply((h0, w0, f0) -> vm1.apply((h1, w1, f1) -> {
			if (h0 == h1 && w0 == w1)
				return VirtualMatrix.of(h0, w0, fun);
			else
				return Fail.t("wrong input sizes");
		}));
	}

}
