package suite.ebnf;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;

public class EbnfOperatorTest {

	@Test
	public void testEbnf() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader(ebnf()));
		System.out.println(ebnf.parse("e0", "1 * 2 + 3"));
	}

	@Test
	public void testLr() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of(ebnf());
		System.out.println(elp.parse("e0", "1 * 2 + 3"));
	}

	private String ebnf() {
		StringBuilder sb = new StringBuilder();
		int i = 0;

		for (TermOp operator : TermOp.values()) {
			String op = "\"" + operator.getName().trim() + "\"";
			String v = v(i++);
			String v1 = v(i);
			if (operator.getAssoc() == Assoc.LEFT)
				sb.append(v + " ::= " + v1 + " | " + v + " " + op + " " + v1 + "\n");
			else
				sb.append(v + " ::= " + v1 + " | " + v1 + " " + op + " " + v + "\n");
		}

		String vx = v(i);
		sb.append(vx + " ::= \"1\" | \"2\" | \"3\" | \"(\" " + v(0) + " \")\"\n");

		String s = sb.toString();
		System.out.println(s);

		return s;
	}

	private String v(int i) {
		return "e" + i;
	}

}
