package suite.ebnf;

import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import suite.ebnf.Ebnf.Ast;
import suite.util.String_;

public class Dump {

	private String in;
	private Writer w = new StringWriter();

	public Dump(Ast ast, String in) {
		this.in = in;
		rethrow(() -> {
			prettyPrint(ast, "");
			return ast;
		});
	}

	public String toString() {
		return w.toString();
	}

	private void prettyPrint(Ast ast, String indent) throws IOException {
		var entity0 = ast.entity;
		List<Ast> children;

		while ((children = ast.children).size() == 1)
			ast = children.get(0);

		if (children.size() != 1) {
			var indent1 = indent + "  ";
			var entity1 = ast.entity;
			var start = ast.getStart();
			var end = ast.getEnd();

			w.write(indent + entity0);
			if (!String_.equals(entity0, entity1))
				w.write(".." + entity1);
			w.write("@" + start + "-" + end);
			if (children.isEmpty())
				w.write("[" + in.substring(start, end) + "]");
			w.write("\n");

			for (var child : children)
				prettyPrint(child, indent1);
		} else
			prettyPrint(children.get(0), indent);
	}

}
