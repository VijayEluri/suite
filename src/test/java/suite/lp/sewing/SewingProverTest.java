package suite.lp.sewing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Generalizer;
import suite.lp.kb.RuleSet;
import suite.lp.sewing.impl.SewingProverImpl;

public class SewingProverTest {

	@Test
	public void test() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "yes");

		SewingProver sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.compile(Suite.parse("yes")).apply(pc));
		assertTrue(sp.compile(Suite.parse("fail; yes")).apply(pc));
		assertTrue(sp.compile(Suite.parse("yes; yes")).apply(pc));
		assertFalse(sp.compile(Suite.parse("yes, fail")).apply(pc));

		assertFalse(sp.compile(Suite.parse("!, fail; yes")).apply(pc));

		assertTrue(sp.compile(new Generalizer().generalize(Suite.parse("(.v = 1; .v = 2), .v = 2"))).apply(pc));
		assertFalse(sp.compile(new Generalizer().generalize(Suite.parse("once (.v = 1; .v = 2), .v = 2"))).apply(pc));

		assertFalse(sp.compile(Suite.parse("not yes")).apply(pc));
		assertTrue(sp.compile(Suite.parse("not fail")).apply(pc));

		assertTrue(sp.compile(new Generalizer().generalize(Suite.parse(".p = yes, .p"))).apply(pc));
	}

	@Test
	public void testBacktrack() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "mem (.e, _) .e");
		Suite.addRule(rs, "mem (_, .tail) .e :- mem .tail .e");
		Suite.addRule(rs, "q .c .v :- once (mem (0,) .v), .a/.b/.c = 0/0/0; mem (1,) .v, .a/.b/.c = 1/1/1");
		Suite.addRule(rs, "r .c :- q .c .v, .v = 1");

		SewingProver sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.compile(new Generalizer().generalize(Suite.parse("r .c"))).apply(pc));
	}

	@Test
	public void testCut() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a :- b");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b :- !, fail");

		SewingProver sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.compile(Suite.parse("a")).apply(pc));
	}

	@Test
	public void testEnv() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "a :- b .a, b .b");
		Suite.addRule(rs, "b 1");

		SewingProver sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.compile(Suite.parse("a")).apply(pc));
	}

	@Test
	public void testIf() {
		RuleSet rs = Suite.createRuleSet();
		SewingProver sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.compile(Suite.parse("if () () fail")).apply(pc));
		assertFalse(sp.compile(Suite.parse("if () fail ()")).apply(pc));
		assertTrue(sp.compile(Suite.parse("if fail fail ()")).apply(pc));
		assertFalse(sp.compile(Suite.parse("if fail () fail")).apply(pc));
	}

}
