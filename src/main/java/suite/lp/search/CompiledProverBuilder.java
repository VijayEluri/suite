package suite.lp.search;

import suite.Suite;
import suite.instructionexecutor.InstructionExecutor;
import suite.instructionexecutor.LogicInstructionExecutor;
import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Node;
import suite.util.FunUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class CompiledProverBuilder implements Builder {

	private ProverConfig proverConfig;
	private Finder compiler;

	/**
	 * Creates a builder that interpretes the logic compiler to compile the
	 * given code, then execute.
	 */
	public static CompiledProverBuilder level1(ProverConfig proverConfig, boolean isDumpCode) {
		return new CompiledProverBuilder(new InterpretedProverBuilder(proverConfig), proverConfig, isDumpCode);
	}

	/**
	 * Creates a builder that compiles the logic compiler, execute it to compile
	 * the given code, then execute.
	 */
	public static CompiledProverBuilder level2(ProverConfig proverConfig, boolean isDumpCode) {
		return new CompiledProverBuilder(level1(proverConfig, false), proverConfig, isDumpCode);
	}

	private CompiledProverBuilder(Builder builder, ProverConfig proverConfig, boolean isDumpCode) {
		this.compiler = createCompiler(builder, isDumpCode);
		this.proverConfig = proverConfig;
	}

	@Override
	public Finder build(RuleSet ruleSet, Node goal) {
		Node goal1 = Suite.substitute(".0 >> .1", Suite.ruleSetToNode(ruleSet), goal);
		final Node code = compile(goal1);
		final ProverConfig proverConfig1 = new ProverConfig(ruleSet, proverConfig);

		return new Finder() {
			public void find(Source<Node> source, Sink<Node> sink) {
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);
				Prover prover = new Prover(proverConfig1);

				try (InstructionExecutor executor = new LogicInstructionExecutor(code, prover)) {
					executor.execute();
				}
			}
		};
	}

	private Node compile(Node program) {
		final Node holder[] = new Node[] { null };

		compiler.find(FunUtil.source(program), new Sink<Node>() {
			public void sink(Node node) {
				holder[0] = new Cloner().clone(node);
			}
		});

		Node code = holder[0];

		if (code != null)
			return code;
		else
			throw new RuntimeException("Logic compilation error");
	}

	private Finder createCompiler(Builder builder, boolean isDumpCode) {
		String compile = "source .in, compile-logic .in .out";
		compile += isDumpCode ? ", pretty.print .out" : "";
		compile += ", sink .out";
		return builder.build(Suite.logicCompilerRuleSet(), Suite.parse(compile));
	}

}
