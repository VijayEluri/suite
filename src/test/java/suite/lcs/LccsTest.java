package suite.lcs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.util.To;

public class LccsTest {

	@Test
	public void test() {
		var lccs = new Lccs();
		var result = lccs.lccs(To.bytes("abczzzzz"), To.bytes("zzzzzabc"));

		assertEquals(3, result.t0.start);
		assertEquals(8, result.t0.end);
		assertEquals(0, result.t1.start);
		assertEquals(5, result.t1.end);
	}

}
