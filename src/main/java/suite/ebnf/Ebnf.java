package suite.ebnf;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
import suite.node.parser.FactorizeResult;
import suite.node.parser.FactorizeResult.FTerminal;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Util;

/**
 * Parser for Backus-Naur form grammars.
 *
 * TODO cyclic problem:
 *
 * primary-no-array-creation-expression ::= simple-name | member-access
 *
 * member-access ::= primary-expression "." identifier (type-argument-list)?
 *
 * primary-expression ::= primary-no-array-creation-expression
 *
 * @author ywsing
 */
public class Ebnf {

	private EbnfBreakdown breakdown = new EbnfBreakdown();
	private EbnfParse engine;

	public static class Node {
		public int start, end;
		public final String entity;
		public final List<Node> nodes;

		public Node(String entity, int start) {
			this(entity, start, 0);
		}

		public Node(String entity, int start, int end) {
			this(entity, start, end, new ArrayList<>());
		}

		public Node(String entity, int start, int end, List<Node> nodes) {
			this.entity = entity;
			this.start = start;
			this.end = end;
			this.nodes = nodes;
		}

		public String toString() {
			return entity + "@" + start + "-" + end + nodes;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	public Ebnf(Reader reader) throws IOException {
		this(reader, EbnfTopDownParse::new);
	}

	public Ebnf(Reader reader, Fun<Map<String, EbnfGrammar>, EbnfParse> fun) throws IOException {
		List<Pair<String, String>> pairs = Read.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(o -> o.fold("", String::concat)) //
				.map(line -> Util.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.toList();

		Map<String, EbnfGrammar> grammarsByEntity = Read.from(pairs) //
				.map(lr -> Pair.of(lr.t0, breakdown.breakdown(lr.t0, lr.t1))) //
				.collect(As::map);

		EbnfHeadRecursion headRecursion = new EbnfHeadRecursion(grammarsByEntity);

		for (Entry<String, EbnfGrammar> entry : grammarsByEntity.entrySet())
			entry.setValue(headRecursion.reduceHeadRecursion(entry.getValue()));

		engine = fun.apply(grammarsByEntity);
	}

	public FactorizeResult parseFNode(String s, String entity) {
		char[] cs = s.toCharArray();
		return toFactorizeResult(cs, 0, cs.length, parse(entity, s));
	}

	private FactorizeResult toFactorizeResult(char cs[], int p0, int px, Node node) {
		List<Node> nodes = node.nodes;
		int size = nodes.size();

		if (size > 0) {
			List<FactorizeResult> frs = new ArrayList<>();
			int pos = p0;
			for (int i = 0; i < size; i++) {
				Node child = nodes.get(i);
				int pos0 = pos;
				pos = i != size - 1 ? child.end : px;
				frs.add(toFactorizeResult(cs, pos0, pos, child));
			}
			return FactorizeResult.merge(node.entity, frs);
		} else {
			Chars pre = Chars.of(cs, p0, node.start);
			Chars mid = Chars.of(cs, node.start, node.end);
			Chars post = Chars.of(cs, node.end, px);
			return new FactorizeResult(pre, new FTerminal(mid), post);
		}
	}

	public Node check(String entity, String s) {
		return engine.check(breakdown.breakdown(entity), s);
	}

	public Node parse(String entity, String s) {
		return engine.parse(breakdown.breakdown(entity), s);
	}

}
