package suite;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import suite.fp.FunCompilerConfig;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.instructionexecutor.ThunkUtil;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder;
import suite.node.Atom;
import suite.node.Node;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.Memoize;
import suite.util.Pair;
import suite.util.Util;

public class EvaluateUtil {

	private Fun<Boolean, Node> fccNodeFun = Memoize.byInput(isLazy -> {
		Atom mode = Atom.of(isLazy ? "LAZY" : "EAGER");

		return new Specializer().specialize(Suite.substitute("" //
				+ "source .in, compile-function .0 .in .out, sink .out", mode));
	});

	// Using level 1 CompiledProverBuilder would break the test case
	// FunRbTreeTest. It would blow up the stack in InstructionExecutor
	private Fun<Pair<ProverConfig, Node>, Finder> fccFinderFun = Memoize.byInput(pair -> {
		Builder builder = new SewingProverBuilder(pair.t0);
		// Builder builder = new InterpretedProverBuilder(pair.t0);
		// Builder builder = new CompiledProverBuilder.level1(pair.t0);
			return builder.build(Suite.funCompilerRuleSet(), pair.t1);
		});

	public boolean proveLogic(Node lp) {
		Builder builder = CompiledProverBuilder.level1(new ProverConfig());
		return proveLogic(builder, Suite.createRuleSet(), lp);
	}

	public boolean proveLogic(RuleSet rs, Node lp) {
		return proveLogic(new InterpretedProverBuilder(), rs, lp);
	}

	public boolean proveLogic(Builder builder, RuleSet rs, Node lp) {
		Node goal = Suite.substitute(".0, sink ()", lp);
		return !evaluateLogic(builder, rs, goal).isEmpty();
	}

	public List<Node> evaluateLogic(Builder builder, RuleSet rs, Node lp) {
		return FindUtil.collectList(builder.build(rs, lp), Atom.NIL);
	}

	public Node evaluateFun(FunCompilerConfig fcc) {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			Node result = executor.execute();
			return fcc.isLazy() ? ThunkUtil.yawnFully(executor.getYawnFun(), result) : result;
		}
	}

	public void evaluateFunToWriter(FunCompilerConfig fcc, Writer writer) throws IOException {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			executor.executeToWriter(writer);
		}
	}

	private FunInstructionExecutor configureFunExecutor(FunCompilerConfig fcc) {
		Node node = fccNodeFun.apply(fcc.isLazy());
		Node code = doFcc(node, fcc);

		if (code != null)
			return new FunInstructionExecutor(code, fcc.isLazy());
		else
			throw new RuntimeException("Function compilation failure");
	}

	public Node evaluateFunType(FunCompilerConfig fcc) {
		Node node = Suite.parse("" //
				+ "source .in" //
				+ ", fc-parse .in .p" //
				+ ", fc-infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", fc-resolve-type-rules .tr" //
				+ ", fc-parse-type .out .t" //
				+ ", sink .out");

		Node type = doFcc(node, fcc);

		if (type != null)
			return type;
		else
			throw new RuntimeException("Type inference failure");
	}

	private Node doFcc(Node compileNode, FunCompilerConfig fcc) {
		return LogUtil.duration("Code compiled", () -> {
			ProverConfig pc = fcc.getProverConfig();
			Finder finder = fccFinderFun.apply(Pair.of(pc, compileNode));
			return FindUtil.collectSingle(finder, appendLibraries(fcc));
		});
	}

	private Node appendLibraries(FunCompilerConfig fcc) {
		Node node = fcc.getNode();
		List<String> libraries = fcc.getLibraries();

		for (int i = libraries.size() - 1; i >= 0; i--) {
			String library = libraries.get(i);
			if (!Util.isBlank(library))
				node = Suite.substitute("using .0 >> .1", Atom.of(library), node);
		}

		return node;
	}

}
