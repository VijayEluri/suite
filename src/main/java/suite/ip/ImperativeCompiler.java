package suite.ip;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import suite.Suite;
import suite.asm.StackAssembler;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder;
import suite.node.Node;
import suite.os.FileUtil;
import suite.parser.IncludePreprocessor;
import suite.primitive.Bytes;
import suite.text.Preprocess;

public class ImperativeCompiler {

	private RuleSet ruleSet = Suite.imperativeCompilerRuleSet();
	private Finder finder = new SewingProverBuilder().build(ruleSet).apply(Suite.parse("" //
			+ "source .ip" //
			+ ", ic-compile 0 .ip .code/()" //
			+ ", sink .code"));

	public Bytes compile(int org, Path path) throws IOException {
		String s0 = FileUtil.read(path);
		String s1 = Preprocess.transform(Arrays.asList(new IncludePreprocessor(path.getParent())), s0).t0;
		return compile(org, s1);
	}

	public Bytes compile(int org, String ip) {
		Node code = FindUtil.collectSingle(finder, Suite.parse(ip));
		return new StackAssembler(32).assemble(Suite.substitute(".0, .1", Suite.parse(".org = " + org), code));
	}

}
