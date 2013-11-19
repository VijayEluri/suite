package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayHitDetail;
import suite.rt.RayTracer.RayTraceObject;

public class Sphere implements RayTraceObject {

	private Vector centre;
	private float radius;

	public Sphere(Vector centre, float radius) {
		this.centre = centre;
		this.radius = radius;
	}

	@Override
	public RayHit hit(final Ray ray) {
		float a = Vector.normsq(ray.dir);
		Vector start0 = Vector.sub(ray.startPoint, centre);
		float adv; // Distance the ray travelled, positive if hits

		float b = 2 * Vector.dot(start0, ray.dir);
		float c = Vector.normsq(start0) - radius * radius;
		float discriminant = b * b - 4 * a * c;

		if (discriminant > 0) { // Hit?
			float sqrt = (float) Math.sqrt(discriminant);

			if (-b - sqrt > 0)
				adv = (-b - sqrt) / (2f * a);
			else
				adv = (-b + sqrt) / (2f * a);
		} else
			adv = -1f;

		final float advance = adv;

		if (advance > RayTracer.negligibleAdvance)
			return new RayHit() {
				public float advance() {
					return advance;
				}

				public RayHitDetail detail() {
					final Vector hitPoint = ray.hitPoint(advance);

					return new RayHitDetail() {
						public Vector hitPoint() {
							return hitPoint;
						}

						public Vector normal() {
							return Vector.sub(hitPoint, centre);
						}

						public Vector litIndex() {
							return new Vector(0.5f, 0.5f, 0.5f);
						}

						public Vector reflectionIndex() {
							return new Vector(0.5f, 0.5f, 0.5f);
						}

						public Vector refractionIndex() {
							return new Vector(0f, 0f, 0f);
						}
					};
				}
			};
		else
			return null;
	}
}
