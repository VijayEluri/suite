package suite.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.IterativeParser;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermOp;
import suite.util.To;

public class IterativeParserTest {

	private IterativeParser iterativeParser = new IterativeParser(TermOp.values());

	@Test
	public void testParseChar() {
		test("97", "+'a'");
	}

	@Test
	public void testParseNil() {
		test("()");
	}

	@Test
	public void testParseSpacedOperator() {
		test("a, b", "a   ,   b");
	}

	@Test
	public void testParseBraces() {
		test("a {b}");
		test("a b {c}");
	}

	@Test
	public void testParseQuotes() {
		test("''''");
		test("'`' (0 - ())", "`0 -`");
		test("'`' (() - ())", "`-`");
	}

	@Test
	public void testParseColons() {
		test("a:b c:d ():e f:() g", "a:b c:d :e f: g");
		test("cmp/() {0}", "cmp/ {0}");
	}

	@Test
	public void testParseException() {
		try {
			Node node = iterativeParser.parse("(a");
			System.out.println(node);
			assertNotNull(Tree.decompose(node).getLeft());
			throw new AssertionError();
		} catch (RuntimeException ex) {
		}
	}

	@Test
	public void testParseExpression() {
		test("a, b :- 1 + 2 * (3 + 4) / 5 / 6 + 7 #");
	}

	@Test
	public void testParsePredicate() {
		test("length (_, .r) .l1 :- length .r .l0, sum .l1 .l0 1");
	}

	@Test
	public void testParseAuto() throws IOException {
		String in = To.string(new File("src/main/resources/auto.sl"));
		Node node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	@Test
	public void testParseFile() throws IOException {
		String in = To.string(new File("src/main/resources/fc.sl"));
		Node node = iterativeParser.parse(in);
		System.out.println(new PrettyPrinter().prettyPrint(node));
		assertNotNull(Tree.decompose(node));
	}

	private void test(String s) {
		test(s, s);
	}

	private void test(String sx, String s0) {
		String s1 = Formatter.dump(iterativeParser.parse(s0));
		assertEquals(sx, s1);
	}

}
