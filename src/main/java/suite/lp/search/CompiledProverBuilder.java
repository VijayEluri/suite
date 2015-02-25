package suite.lp.search;

import suite.Suite;
import suite.instructionexecutor.InstructionExecutor;
import suite.instructionexecutor.LogicInstructionExecutor;
import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.FunUtil.Fun;
import suite.util.os.LogUtil;

public class CompiledProverBuilder implements Builder {

	private ProverConfig proverConfig;
	private Finder compiler;

	/**
	 * Creates a builder that interpretes the logic compiler to compile the
	 * given code, then execute.
	 */
	public static CompiledProverBuilder level1(ProverConfig proverConfig) {
		return new CompiledProverBuilder(new SewingProverBuilder(proverConfig), proverConfig);
	}

	/**
	 * Creates a builder that compiles the logic compiler, execute it to compile
	 * the given code, then execute.
	 */
	public static CompiledProverBuilder level2(ProverConfig proverConfig) {
		return new CompiledProverBuilder(level1(proverConfig), proverConfig);
	}

	private CompiledProverBuilder(Builder builder, ProverConfig proverConfig) {
		this.compiler = createCompiler(builder);
		this.proverConfig = proverConfig;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		Node rules = Suite.ruleSetToNode(ruleSet);

		return goal -> {
			Node code = compile(Suite.substitute(".0 >> .1", rules, goal));

			return (source, sink) -> {
				ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);

				try (InstructionExecutor executor = new LogicInstructionExecutor(code, proverConfig1)) {
					executor.execute();
				}
			};
		};
	}

	private Node compile(Node program) {
		return LogUtil.duration("Code compiled", () -> FindUtil.collectSingle(compiler, program));
	}

	private Finder createCompiler(Builder builder) {
		String compile = "source .in, compile-logic .in .out, sink .out";
		return builder.build(Suite.logicCompilerRuleSet()).apply(Suite.parse(compile));
	}

}
