package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.compile.impl.CompileGeneralizerImpl;
import suite.lp.doer.GeneralizerFactory.Generalize_;
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
		for (GeneralizerFactory gf : new GeneralizerFactory[] { new CompileGeneralizerImpl(), new SewingGeneralizerImpl(), }) {
			Generalize_ p = gf.generalizer(Suite.parse(pattern));

			assertTrue(Binder.bind(p.apply(gf.env()), Suite.parse(match), new Trail()));
		}
	}

}