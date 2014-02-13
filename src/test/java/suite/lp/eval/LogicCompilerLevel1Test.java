package suite.lp.eval;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Builder;
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
		List<String> filenames = Arrays.asList("auto.sl", "fc.sl");

		Node goal = new Specializer().specialize(Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out" //
		, Atom.create("LAZY")));

		Node input = Suite.parse("1 + 2");
		RuleSet rs = Suite.createRuleSet(filenames);
		Node result = FindUtil.collectSingle(finder(rs, goal), input);

		System.out.println(result);
		assertNotNull(result);
	}

	/**
	 * Call member two times. This test might fail in some poor tail recursion
	 * optimization implementations, as some variables are not unbounded when
	 * backtracking.
	 */
	@Test
	public void testMemberOfMember() {
		RuleSet rs = Suite.createRuleSet();
		Suite.addRule(rs, "member (.e, _) .e");
		Suite.addRule(rs, "member (_, .tail) .e :- member .tail .e");

		Node goal = Suite.parse("source .lln, member .lln .ln, member .ln .n, sink .n");
		Node program = Suite.parse("((1, 2,), (3, 4,),)");
		List<Node> results = FindUtil.collectList(finder(rs, goal), program);

		System.out.println(results);
		assertTrue(results.size() == 4);
	}

	private Finder finder(RuleSet rs, Node goal) {
		Builder builder = CompiledProverBuilder.level1(new ProverConfig(), false);
		return builder.build(rs, goal);
	}

}
