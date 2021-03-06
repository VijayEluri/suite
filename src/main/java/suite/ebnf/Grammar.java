package suite.ebnf;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.String_;

public class Grammar {

	public enum GrammarType {
		AND___, //
		ENTITY, //
		EXCEPT, //
		NAMED_, //
		ONCE__, //
		OPTION, //
		OR____, //
		REPT0_, //
		REPT0H, //
		REPT1_, //
		STRING, //
	}

	public final GrammarType type;
	public final String content;
	public final List<Grammar> children;

	public static Map<String, Grammar> parse(Reader reader) {
		var breakdown = new Breakdown();

		return Read //
				.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(o -> o.fold("", String::concat)) //
				.map(line -> String_.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.collect(As::streamlet) //
				.map2(lr -> lr.t0, lr -> lr.map(breakdown::breakdown)) //
				.toMap();
	}

	public Grammar(GrammarType type) {
		this(type, null, List.of());
	}

	public Grammar(GrammarType type, String content) {
		this(type, content, List.of());
	}

	public Grammar(GrammarType type, Grammar child) {
		this(type, null, child);
	}

	public Grammar(GrammarType type, String content, Grammar child) {
		this(type, content, List.of(child));
	}

	public Grammar(GrammarType type, List<Grammar> children) {
		this(type, null, children);
	}

	public Grammar(GrammarType type, String content, List<Grammar> children) {
		this.type = type;
		this.content = content;
		this.children = children;
	}

	public String describe() {
		return type + (content != null ? "." + content : "") //
				+ (type != GrammarType.NAMED_ //
						? Read.from(children).map(Grammar::describe).collect(As.joinedBy("(", ",", ")")) //
						: "");
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		toString("", sb);
		return sb.toString();
	}

	private void toString(String indent, StringBuilder sb) {
		sb.append(indent + type + " (" + content + ")\n");
		for (var child : children)
			child.toString(indent + "| ", sb);
	}

}
