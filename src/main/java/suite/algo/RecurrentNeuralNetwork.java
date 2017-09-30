package suite.algo;

import java.util.Random;

import suite.math.Forget;
import suite.math.Tanh;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.primitive.Floats_;

public class RecurrentNeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	private float learningRate;
	private int inputLength;
	private int memoryLength;
	private int ll;
	private int ll1;

	public RecurrentNeuralNetwork() {
		this(1f, 8, 8);
	}

	public RecurrentNeuralNetwork(float learningRate, int inputLength, int memoryLength) {
		this.learningRate = learningRate;
		this.inputLength = inputLength;
		this.memoryLength = memoryLength;
		ll = inputLength + memoryLength;
		ll1 = ll + 1;
	}

	public Unit unit() {
		return new Unit();
	}

	public class Unit {
		private float[] memory = new float[memoryLength];
		private float[][] weights = new float[memoryLength][ll1];

		public Unit() {
			Random random = new Random();
			double isll = 1f / Math.sqrt(ll);

			// random weights, bias 0; Xavier initialization
			for (int i = 0; i < memoryLength; i++)
				for (int j = 0; j < ll; j++)
					weights[i][j] = (float) (random.nextGaussian() * isll);
		}

		public float[] activateForward(float[] input) {
			return activate_(input, null);
		}

		public void propagateBackward(float[] input, float[] expected) {
			activate_(input, expected);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("weights = " + mtx.toString(weights));
			sb.append("memory = " + mtx.toString(memory) + "\n");
			return sb.toString();
		}

		private float[] activate_(float[] input, float[] expected) {
			float[] memory0 = memory;
			float[] iv = new float[ll1];

			Floats_.copy(input, 0, iv, 0, inputLength);
			Floats_.copy(memory0, 0, iv, inputLength, memoryLength);
			iv[ll] = 1f;

			float[] memory1 = vec.of(memory = Tanh.tanhOn(mtx.mul(weights, iv)));

			if (expected != null) {
				float[] e_memory1 = vec.sub(expected, memory1);
				float[] e_weights = Forget.forgetOn(e_memory1, Tanh.tanhGradientOn(memory1));

				for (int i = 0; i < memoryLength; i++)
					for (int j = 0; j < ll1; j++)
						weights[i][j] += learningRate * e_weights[i] * iv[j];
			}

			return memory1;
		}
	}

}
