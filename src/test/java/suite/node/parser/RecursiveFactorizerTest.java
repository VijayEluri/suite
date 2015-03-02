package suite.node.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.inspect.Inspect;
import suite.node.Node;
import suite.node.Str;
import suite.node.io.Lister;
import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FTerminal;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.node.util.Rewriter;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Nodify;
import suite.util.To;

public class RecursiveFactorizerTest {

	private RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

	@Test
	public void testParseAuto() throws IOException {
		String s0 = FileUtil.read("src/main/ll/auto.sl").trim();
		FNode fn = recursiveFactorizer.parse(s0);
		String sx = recursiveFactorizer.unparse(fn);
		assertEquals(s0, sx);
	}

	@Test
	public void testRefactorFile() throws IOException {
		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		FNode fnx = transform(fn0);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	@Test
	public void testRefactorFile1() throws IOException {
		Nodify nodify = new Nodify(new Inspect());
		Rewriter rewriter = new Rewriter(new Str("ic-compile0"), new Str("ic-compile1"));

		String s0 = FileUtil.read("src/main/ll/ic/ic.sl").trim();
		FNode fn0 = recursiveFactorizer.parse(s0);
		Node node0 = nodify.nodify(FNode.class, fn0);
		System.out.println(node0);
		Node nodex = rewriter.replace(node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		String sx = recursiveFactorizer.unparse(fnx);
		System.out.println(sx);
	}

	@Test
	public void testRefactorNodify() throws IOException {
		Lister lister = new Lister();
		Nodify nodify = new Nodify(new Inspect());

		String s = "1 + 2 * 3";
		FNode fn = recursiveFactorizer.parse(s);
		Node node = nodify.nodify(FNode.class, fn);
		System.out.println(lister.list(node));
		assertEquals(fn, nodify.unnodify(FNode.class, node));
	}

	private FNode transform(FNode fn) {
		FTerminal from = new FTerminal(To.chars("ic-compile0"));
		FTerminal to = new FTerminal(To.chars("ic-compile1"));

		if (fn.equals(from))
			return to;
		else if (fn instanceof FTree) {
			FTree ft = (FTree) fn;
			return new FTree(ft.type, ft.name, Read.from(ft.fns).map(this::transform).toList());
		} else
			return fn;
	}

}
