package suite.math;

public class Vector {

	private float x, y, z;

	public Vector(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Vector add(Vector u, Vector v) {
		return new Vector(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector neg(Vector v) {
		return new Vector(-v.x, -v.y, -v.z);
	}

	public static float dot(Vector u, Vector v) {
		return u.x * v.x + u.y * v.y + u.z * v.z;
	}

	public static Vector cross(Vector u, Vector v) {
		return new Vector(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
	}

}
