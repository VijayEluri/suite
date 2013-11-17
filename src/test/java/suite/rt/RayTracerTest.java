package suite.rt;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.RayTraceObject;

public class RayTracerTest {

	@Test
	public void test() throws IOException {
		RayTraceObject sphere0 = new Sphere(new Vector(0f, 0f, 5f), 1f);
		RayTraceObject sphere1 = new Sphere(new Vector(-1f, -1f, 3f), 1f);
		RayTraceObject plane = new Plane(new Vector(0f, -1f, 0f), 0f);

		LightSource lighting = new DirectionalLight(new Vector(10000f, 10000f, -10000f), new Vector(1f, 1f, 1f));
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1, plane));

		new RayTracer(Arrays.asList(lighting), scene).trace(640, 480, 640);
	}

}
