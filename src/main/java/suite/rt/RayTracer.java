package suite.rt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import suite.math.Vector;
import suite.util.LogUtil;

/**
 * TODO remove Vector.norm() at Ray constructor and fix maths
 * 
 * TODO fix RayTracerTest.testLight() etc cases black-out issues
 * 
 * TODO test accurate Fresnel (and Schlick's approximation?)
 * 
 * @author ywsing
 */
public class RayTracer {

	public static float negligibleAdvance = 0.0001f;

	private int nThreads = 4;
	private int depth = 4;

	private float refractiveIndex0 = 1f;
	private float refractiveIndex1 = 1.1f;
	private float enterRefractiveRatio = refractiveIndex0 / refractiveIndex1;
	private float exitRefractiveRatio = refractiveIndex1 / refractiveIndex0;

	private float adjustFresnel = 0f;

	private Vector ambient = Vector.origin;

	private Collection<LightSource> lightSources;
	private RtObject scene;

	public interface RtObject {

		/**
		 * Calculates hit point with a ray. Assumes direction is normalized.
		 */
		public List<RayHit> hit(Ray ray);
	}

	public interface RayHit {
		public float advance();

		public RayIntersection intersection();

		public Comparator<RayHit> comparator = (rh0, rh1) -> rh0.advance() < rh1.advance() ? -1 : 1;
	}

	public interface RayIntersection {
		public Vector hitPoint();

		public Vector normal();

		public Material material();
	}

	public interface Material {
		public Vector surfaceColor();

		public boolean isReflective();

		public float transparency();
	}

	public static class Ray {
		public Vector startPoint;
		public Vector dir;

		public Ray(Vector startPoint, Vector dir) {
			this.startPoint = startPoint;
			this.dir = dir;
		}

		public Vector hitPoint(float advance) {
			return Vector.add(startPoint, Vector.mul(dir, advance));
		}

		public String toString() {
			return startPoint + " => " + dir;
		}
	}

	public interface LightSource {
		public Vector source();

		public Vector lit(Vector point);
	}

	public RayTracer(Collection<LightSource> lightSources, RtObject scene) {
		this.lightSources = lightSources;
		this.scene = scene;
	}

	public Vector test() {
		return test(new Ray(Vector.origin, new Vector(0f, 0f, 1f)));
	}

	public Vector test(Ray ray) {
		return traceRay(depth, ray);
	}

	public void trace(BufferedImage bufferedImage, int viewDistance) {
		int width = bufferedImage.getWidth(), height = bufferedImage.getHeight();
		int centreX = width / 2, centreY = height / 2;
		Vector pixels[][] = new Vector[width][height];
		int xs[] = new int[nThreads + 1];

		for (int i = 0; i <= nThreads; i++)
			xs[i] = width * i / nThreads;

		Thread threads[] = new Thread[nThreads];

		for (int i = 0; i < nThreads; i++) {
			int i1 = i;

			threads[i1] = new Thread(() -> {
				for (int x = xs[i1]; x < xs[i1 + 1]; x++)
					for (int y = 0; y < height; y++) {
						Vector lit;

						try {
							Vector startPoint = Vector.origin;
							Vector dir = new Vector(x - centreX, y - centreY, viewDistance);
							lit = traceRay(depth, new Ray(startPoint, dir));
						} catch (Exception ex) {
							LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
							lit = new Vector(1f, 1f, 1f);
						}

						pixels[x][y] = lit;
					}
			});
		}

		for (int i = 0; i < nThreads; i++)
			threads[i].start();

		for (int i = 0; i < nThreads; i++)
			try {
				threads[i].join();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Vector pixel = limit(pixels[x][y]);
				bufferedImage.setRGB(x, y, new Color(pixel.x, pixel.y, pixel.z).getRGB());
			}
	}

	private Vector traceRay(int depth, Ray ray) {
		RayHit rayHit = nearestHit(scene.hit(ray));
		Vector color1;

		if (rayHit != null) {
			RayIntersection i = rayHit.intersection();
			Vector hitPoint = i.hitPoint();
			Vector normal0 = Vector.norm(i.normal());

			float dot0 = Vector.dot(ray.dir, normal0);
			boolean isInside = dot0 > 0f;
			float dot;
			Vector normal;

			if (!isInside) {
				normal = normal0;
				dot = dot0;
			} else {
				normal = Vector.neg(normal0);
				dot = -dot0;
			}

			Material material = i.material();
			Vector color;

			if (depth > 0) {
				float cos = -dot / (float) Math.sqrt(Vector.abs2(ray.dir));

				// Account reflection
				Vector reflectDir = Vector.add(ray.dir, Vector.mul(normal, -2f * dot));
				Vector reflectPoint = Vector.add(hitPoint, negligible(normal));
				Vector reflectColor = traceRay(depth - 1, new Ray(reflectPoint, reflectDir));

				// Account refraction
				float eta = isInside ? exitRefractiveRatio : enterRefractiveRatio;
				float k = 1 - eta * eta * (1 - cos * cos);
				Vector refractColor;

				if (k >= 0) {
					Vector refractDir = Vector.add(Vector.mul(ray.dir, eta), Vector.mul(normal, eta * cos - (float) Math.sqrt(k)));
					Vector refractPoint = Vector.sub(hitPoint, negligible(normal));
					refractColor = traceRay(depth - 1, new Ray(refractPoint, refractDir));
				} else
					refractColor = Vector.origin;

				// Accurate Fresnel equation
				// float cos1 = (float) Math.sqrt(k);
				// float f0 = (eta * cos - cos1) / (eta * cos + cos1);
				// float f1 = (cos - eta * cos1) / (cos + eta * cos1);
				// float accurateFresnel = (f0 * f0 + f1 * f1) / 2f;

				// Schlick approximation
				// float mix = (float) Math.pow((refractiveIndex0 -
				// refractiveIndex1) / (refractiveIndex0 +
				// refractiveIndex1), 2f);
				// float cos1 = 1 - cos;
				// float cos2 = cos1 * cos1;
				// float fresnel = mix + (1 - mix) * cos1 * cos2 * cos2;

				// Not even Schlick - copied code
				float cos1 = 1 - cos;
				float cos2 = cos1 * cos1;
				float fresnel = 0.1f + 0.9f * cos1 * cos2; // * cos2;

				// Fresnel is often too low. Mark it up for visual effect.
				float fresnel1 = adjustFresnel + fresnel * (1 - adjustFresnel);

				color = Vector.add(Vector.mul(reflectColor, fresnel1),
						Vector.mul(refractColor, (1f - fresnel1) * material.transparency()));
			} else {
				color = Vector.origin;

				// Account light sources
				for (LightSource lightSource : lightSources) {
					Vector lightDir = Vector.sub(lightSource.source(), hitPoint);
					float lightDot = Vector.dot(lightDir, normal);

					if (lightDot > 0) { // Facing the light
						Vector lightPoint = Vector.add(hitPoint, negligible(normal));
						RayHit lightRayHit = nearestHit(scene.hit(new Ray(lightPoint, lightDir)));

						if (lightRayHit == null || lightRayHit.advance() > 1f) {
							Vector lightColor = lightSource.lit(hitPoint);
							float cos = lightDot / (float) (Math.sqrt(Vector.abs2(lightDir)));
							color = Vector.add(color, Vector.mul(lightColor, cos));
						}
					}
				}
			}

			color1 = mc(color, material.surfaceColor());
		} else
			color1 = ambient;

		return color1;
	}

	private RayHit nearestHit(List<RayHit> rayHits) {
		return RayUtil.filterRayHits(rayHits).minOrNull(RayHit.comparator);
	}

	/**
	 * Multiply vector components.
	 */
	private static Vector mc(Vector u, Vector v) {
		return new Vector(u.x * v.x, u.y * v.y, u.z * v.z);

	}

	private static Vector negligible(Vector v) {
		return new Vector(negligible(v.x), negligible(v.y), negligible(v.z));
	}

	private static float negligible(float f) {
		if (f > 0f)
			return negligibleAdvance;
		else if (f < 0f)
			return -negligibleAdvance;
		else
			return 0f;
	}

	/**
	 * Limit vector components between 0 and 1.
	 */
	private static Vector limit(Vector u) {
		return new Vector(limit(u.x), limit(u.y), limit(u.z));
	}

	private static float limit(float f) {
		return Math.min(1f, Math.max(0f, f));
	}

	public void setnThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setAdjustFresnel(float adjustFresnel) {
		this.adjustFresnel = adjustFresnel;
	}

	public void setAmbient(Vector ambient) {
		this.ambient = ambient;
	}

}
