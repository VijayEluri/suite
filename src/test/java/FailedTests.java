import java.io.IOException;

import org.instructionexecutor.FunRbTreeTest;
import org.instructionexecutor.InstructionTranslatorTest;
import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel2;
import org.suite.search.ProverBuilder.Builder;

public class FailedTests {

	// Need to increase InstructionExecutor.stackSize, or implement tail
	// recursion
	@Test
	public void test0() {
		RuleSet rs = Suite.nodeToRuleSet(Suite.parse("" //
				+ "member (.e, _) .e #" //
				+ "member (_, .tail) .e :- member .tail .e #" //
				+ "sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #" //
				+ "sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #" //
				+ "sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #" //
		));

		Node goal = Suite.parse("(), sink ()");
		Builder builder = new CompiledProverBuilderLevel2(new ProverConfig(), false);
		Suite.evaluateLogic(builder, rs, goal);
	}

	// Type check take 11 seconds
	@Test
	public void test1() throws IOException {
		new FunRbTreeTest().test();
	}

	// Strange error message "Unknown expression if b"
	@Test
	public void test2() throws IOException {
		Suite.evaluateFun("if a then b", false);
	}

	// Code too large
	@Test
	public void test3() throws IOException {
		new InstructionTranslatorTest().testEagerFunctional();
	}


	// should be something like list-of .t => list-of .t => boolean
	@Test
	public void test4() {
		System.out.println(Suite.evaluateFunType("using STANDARD >> ends-with"));
	}

	// should be something like .t => list-of list-of .t => list-of .t
	@Test
	public void test5() {
		System.out.println(Suite.evaluateFunType("using STANDARD >> join"));
	}

}
