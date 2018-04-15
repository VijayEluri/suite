package suite.ip;

import java.nio.file.Path;
import java.util.List;

import suite.Suite;
import suite.asm.StackAssembler;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.parser.IncludePreprocessor;
import suite.primitive.Bytes;
import suite.text.Preprocess;
import suite.util.To;

public class ImperativeCompiler {

	private RuleSet ruleSet = Suite.imperativeCompilerRuleSet();

	private Finder finder = new SewingProverBuilder2() //
			.build(ruleSet) //
			.apply(Suite.parse("" //
					+ "source .ip" //
					+ ", compile-imperative .ip .code/()" //
					+ ", sink .code"));

	public Bytes compile(int org, Path path) {
		var s0 = To.string(path);
		var s1 = Preprocess.transform(List.of(new IncludePreprocessor(path.getParent())::preprocess), s0).t0;
		return compile(org, s1);
	}

	public Bytes compile(int org, String ip) {
		var code = finder.collectSingle(Suite.parse(ip));
		return new StackAssembler(32).assembler.assemble(Suite.substitute(".0, .1", Suite.parse(".org = " + org), code));
	}

}
