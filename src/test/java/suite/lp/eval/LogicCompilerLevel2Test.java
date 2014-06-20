package suite.lp.eval;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Builder;

public class LogicCompilerLevel2Test {

	// Require tail recursion to work
	@Test
	public void test0() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		Builder builder = CompiledProverBuilder.level2(new ProverConfig());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

	@Test
	public void test1() throws IOException {
		RuleSet rs = Suite.createRuleSet();
		Suite.importResource(rs, "auto.sl");

		Builder builder = CompiledProverBuilder.level2(new ProverConfig());
		Suite.evaluateLogic(builder, rs, "(), sink ()");
	}

}
