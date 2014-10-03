package suite.lp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Node;

public class LogicCompilerLevel1Test {

	/**
	 * Compiles the functional compiler and use it to compile a simple
	 * functional program.
	 */
	@Test
	public void testCompileFunProgram() {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl", "fc/fc.sl"));

		Node goal = new Specializer().specialize(Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out" //
		, Atom.of("LAZY")));

		Node input = Suite.parse("1 + 2");
		Node result = FindUtil.collectSingle(finder(rs, goal), input);

		System.out.println(result);
		assertNotNull(result);
	}

	@Test
	public void testMemberOfMember() {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl"));
		Node goal = Suite.parse("source .lln, member .lln .ln, member .ln .n, sink .n");
		Node input = Suite.parse("((1, 2,), (3, 4,),)");
		List<Node> results = FindUtil.collectList(finder(rs, goal), input);

		System.out.println(results);
		assertTrue(results.size() == 4);
	}

	/**
	 * This test might fail in some poor tail recursion optimization
	 * implementations, as some variables are not unbounded when backtracking.
	 */
	@Test
	public void testTailCalls() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "ab a");
		Suite.addRule(rs, "ab b");

		Node goal = Suite.parse("ab .a, ab .b, sink (.a, .b,)");
		List<Node> results = FindUtil.collectList(finder(rs, goal), Atom.NIL);

		System.out.println(results);
		assertTrue(results.size() == 4);
	}

	private Finder finder(RuleSet rs, Node goal) {
		return CompiledProverBuilder.level1(new ProverConfig()).build(rs).apply(goal);
	}

}
