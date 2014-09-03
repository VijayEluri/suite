package suite.text;

import suite.primitive.Bytes;

public class RollingHashUtil {

	/**
	 * Calculates hash value for some bytes.
	 */
	public int hash(Bytes bytes) {
		int rollingHash = 0;
		for (byte b : bytes.toBytes())
			rollingHash = roll(rollingHash, b);
		return rollingHash;
	}

	/**
	 * Rolls (adds) a new byte into a hashed value.
	 */
	public int roll(int rollingHash, byte b) {
		return Integer.rotateLeft(rollingHash ^ b, 1);
	}

	/**
	 * Unrolls the last byte in the sliding window.
	 */
	public int unroll(int rollingHash, byte b) {
		return Integer.rotateRight(rollingHash, 1) ^ b;
	}

	/**
	 * Unrolls the first byte in the sliding window.
	 */
	public int unroll(int rollingHash, byte b, int windowSize) {
		int b1 = Integer.rotateLeft(b, windowSize);
		return rollingHash ^ b1;
	}

}
