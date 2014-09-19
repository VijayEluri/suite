package suite.lp.doer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.lp.SewingProver;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;

public class ProverTest {

	@Test
	public void testAppend() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "app () .l .l");
		Suite.addRule(rs, "app (.h, .r) .l (.h, .r1) :- app .r .l .r1");

		assertTrue(test(rs, "app (a, b, c,) (d, e,) (a, b, c, d, e,)"));
	}

	@Test
	public void testCut() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a :- !, fail");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "yes");

		assertFalse(test(rs, "a"));
		assertFalse(Suite.proveLogic(rs, "cut.begin .c, (nl, cut.end .c, fail; yes)"));
		assertTrue(Suite.proveLogic(rs, "(cut.begin .c, nl, cut.end .c, fail); yes"));
	}

	@Test
	public void testFindAll() {
		RuleSet rs = Suite.createRuleSet();
		assertTrue(test(rs, "find.all .v (.v = a; .v = b; .v = c) .results, .results = (a, b, c,)"));
	}

	@Test
	public void testIsCyclic() {
		RuleSet rs = Suite.createRuleSet();
		assertFalse(test(rs, ".a = (a, b, c,), is.cyclic .a"));
		assertTrue(test(rs, ".a = (a, b, .a, c,), is.cyclic .a"));
	}

	@Test
	public void testMember() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "mem ((.e, _), .e)");
		Suite.addRule(rs, "mem ((_, .remains), .e) :- mem (.remains, .e)");

		assertTrue(test(rs, "mem ((a, ), a)"));
		assertTrue(test(rs, "mem ((a, b, c, ), .v)"));
		assertTrue(test(rs, ".l = (1, 2, 3,), find.all .v (mem (.l, .v)) .l"));
		assertFalse(test(rs, "mem ((a, b, c, ), d)"));
	}

	@Test
	public void testNotNot() throws IOException {
		assertTrue(test(Suite.createRuleSet(), "not not (.a = 3), not bound .a"));
	}

	@Test
	public void testProve() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b");
		Suite.addRule(rs, "c");
		Suite.addRule(rs, "a b .v :- fail");
		Suite.addRule(rs, "a b c");
		Suite.addRule(rs, ".var is a man");

		assertTrue(Suite.proveLogic(rs, ""));
		assertTrue(Suite.proveLogic(rs, "a"));
		assertTrue(Suite.proveLogic(rs, "a, b"));
		assertTrue(Suite.proveLogic(rs, "a, b, c"));
		assertTrue(Suite.proveLogic(rs, "a, fail; b"));
		assertTrue(Suite.proveLogic(rs, "a b c"));
		assertTrue(Suite.proveLogic(rs, "abc is a man"));
		assertTrue(Suite.proveLogic(rs, ".v = a, .v = b; .v = c"));
		assertTrue(Suite.proveLogic(rs, "[1, 2, 3] = [1, 2, 3]"));

		assertFalse(Suite.proveLogic(rs, "fail"));
		assertFalse(Suite.proveLogic(rs, "d"));
		assertFalse(Suite.proveLogic(rs, "a, fail"));
		assertFalse(Suite.proveLogic(rs, "fail, a"));
		assertFalse(Suite.proveLogic(rs, "a b d"));
		assertFalse(Suite.proveLogic(rs, "a = b"));
		assertFalse(Suite.proveLogic(rs, ".v = a, .v = b"));
	}

	@Test
	public void testSystemPredicates() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "mem ((.e, _), .e)");
		Suite.addRule(rs, "mem ((_, .remains), .e) :- mem (.remains, .e)");

		assertTrue(test(rs, ".l = (1, 2,), find.all .v (mem (.l, .v)) .l"));
	}

	@Test
	public void testTree() {
		RuleSet rs = Suite.createRuleSet();
		assertFalse(test(rs, "tree .t0 a ':' b, tree .t1 a ':' b, same .t0 .t1"));
		assertTrue(test(rs, "tree.intern .t0 a ':' b, tree.intern .t1 a ':' b, same .t0 .t1"));
	}

	@Test
	public void testWrite() {
		RuleSet rs = Suite.createRuleSet();
		assertTrue(test(rs, "write (1 + 2 * 3), nl"));
		assertTrue(test(rs, "write \"Don\"\"t forget%0A4 Jun 1989\", nl"));
	}

	private boolean test(RuleSet rs, String lp) {
		boolean b0 = Suite.proveLogic(rs, lp);
		boolean b1 = new SewingProver(rs).compile(Suite.parse(lp)).apply(new ProverConfig(rs));
		if (b0 == b1)
			return b0;
		else
			throw new RuntimeException("Different prove result");
	}

}
