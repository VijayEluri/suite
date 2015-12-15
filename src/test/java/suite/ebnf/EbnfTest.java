package suite.ebnf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import suite.node.parser.FactorizeResult;
import suite.os.FileUtil;

public class EbnfTest {

	@Test
	public void testExcept() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "non-alphas ::= (non-alpha)* \n" //
				+ "non-alpha ::= <CHARACTER> /except/ ([a-z] | [A-Z]) \n" //
				+ "non-boolean ::= <IDENTIFIER> /except/ (\"true\" | \"false\") \n" //
		));
		assertNotNull(ebnf.check("non-alphas", "123!@#"));
		assertNotNull(ebnf.check("non-boolean", "beatles"));
		assertNull(ebnf.check("non-alphas", "456q$%^"));
		assertNull(ebnf.check("non-boolean", "false"));
	}

	@Test
	public void testExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/expression.ebnf"));
		System.out.println(ebnf.parse("<expression>", "1 + 2 + 3"));
	}

	@Test
	public void testHeadRecursion() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "number ::= number \"x\" digit | digit \n" //
				+ "digit ::= [0-9] \n" //
		));
		System.out.println(ebnf.parse("number", "1"));
		System.out.println(ebnf.parse("number", "1x2"));
		System.out.println(ebnf.parse("number", "1x2x3x4"));
	}

	@Test
	public void testId() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("<IDENTIFIER>", "abc"));
	}

	@Test
	public void testJava() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		String s = FileUtil.read("src/test/java/suite/ebnf/EbnfTest.java");
		System.out.println(new EbnfDump(ebnf.parse("CompilationUnit", s), s));
	}

	@Test
	public void testJavaExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("Expression", "\"1\" + \"2\""));
	}

	@Test
	public void testJavaSimple() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("CompilationUnit", "public class C { public void f() { int a; } }"));
	}

	@Test
	public void testRefactor() throws IOException {
		String sql0 = "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL) ORDER BY COL DESC";
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));
		FactorizeResult fr = rewrite(ebnf, "intersect-select" //
				, "SELECT .0 FROM DUAL" //
				, "SELECT .0 FROM DUAL WHERE COL2 = 1" //
				, ebnf.parseFNode(sql0, "sql"));
		String sql1 = fr.unparse();
		assertEquals(sql1, "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL WHERE COL2 = 1) ORDER BY COL DESC");
	}

	@Test
	public void testSql() throws IOException {
		String sql = "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL) ORDER BY COL DESC";
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));
		System.out.println(ebnf.parse("sql", sql));
	}

	private FactorizeResult rewrite(Ebnf ebnf, String entity, String from, String to, FactorizeResult fr0) {
		FactorizeResult frfrom = ebnf.parseFNode(from, entity);
		FactorizeResult frto = ebnf.parseFNode(to, entity);
		return FactorizeResult.rewrite(frfrom, frto, fr0);
	}

}
