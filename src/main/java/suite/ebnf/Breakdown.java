package suite.ebnf;

import java.util.Arrays;
import java.util.List;

import suite.adt.Pair;
import suite.ebnf.Grammar.GrammarType;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Read;
import suite.util.ParseUtil;
import suite.util.Util;

public class Breakdown {

	public Grammar breakdown(String name, String s) {
		return new Grammar(GrammarType.NAMED_, name, breakdown(s));
	}

	public Grammar breakdown(String s) {
		Grammar eg;
		List<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if (1 < (list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.OR____, breakdown(list));
		else if ((pair = ParseUtil.search(s, " /except/ ", Assoc.RIGHT)) != null)
			eg = new Grammar(GrammarType.EXCEPT, Arrays.asList(breakdown(pair.t0), breakdown(pair.t1)));
		else if (1 < (list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.AND___, breakdown(list));
		else if (s.equals(""))
			eg = new Grammar(GrammarType.AND___);
		else if (s.endsWith("?"))
			eg = new Grammar(GrammarType.OPTION, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("*"))
			eg = new Grammar(GrammarType.REPT0_, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("+"))
			eg = new Grammar(GrammarType.REPT1_, breakdown(Util.substr(s, 0, -1)));
		else if (s.startsWith("\"") && s.endsWith("\""))
			eg = new Grammar(GrammarType.STRING, Escaper.unescape(Util.substr(s, 1, -1), "\""));
		else if (s.startsWith("(") && s.endsWith(")"))
			eg = breakdown(Util.substr(s, 1, -1));
		else
			eg = new Grammar(GrammarType.ENTITY, s);

		return eg;
	}

	private List<Grammar> breakdown(List<String> list) {
		return Read.from(list).map(this::breakdown).toList();
	}

}
