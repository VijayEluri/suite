package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.compile.impl.CompileGeneralizerImpl;
import suite.lp.sewing.impl.SewingGeneralizerImpl;

public class GeneralizerFactoryTest {

	@Test
	public void test0() {
		test("mem ((.e, _), .e)", "mem ((a, ), a)");
	}

	@Test
	public void test1() {
		test(".e .e", "a a");
	}

	private void test(String pattern, String match) {
		for (var gf : new GeneralizerFactory[] { new CompileGeneralizerImpl(), new SewingGeneralizerImpl(), }) {
			var p = gf.generalizer(Suite.parse(pattern));

			assertTrue(Binder.bind(p.apply(gf.mapper().env()), Suite.parse(match), new Trail()));
		}
	}

}
