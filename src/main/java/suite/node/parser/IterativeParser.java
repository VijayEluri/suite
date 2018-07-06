package suite.node.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.parser.Lexer.Token;
import suite.node.tree.TreeTuple;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.streamlet.FunUtil.Sink;
import suite.text.Preprocess;
import suite.util.Fail;
import suite.util.String_;

/**
 * Non-recursive, performance-improved parser for operator-based languages.
 *
 * @author ywsing
 */
public class IterativeParser {

	private TerminalParser terminalParser;
	private Operator[] operators;
	private boolean isSpecialBraces;

	public IterativeParser(Operator[] operators) {
		this(operators, true);
	}

	public IterativeParser(Operator[] operators, boolean isSpecialBraces) {
		this(Singleton.me.grandContext, operators, isSpecialBraces);
	}

	private IterativeParser(Context context, Operator[] operators, boolean isSpecialBraces) {
		this.operators = operators;
		terminalParser = new TerminalParser(context);
		this.isSpecialBraces = isSpecialBraces;
	}

	private class Section {
		private char kind;
		private Deque<Tree> list = new ArrayDeque<>(List.of(Tree.of(null, null, Atom.NIL)));
		private boolean isDanglingRight = true;

		public Section(char kind) {
			this.kind = kind;
		}

		private Tree unwind(Operator operator) {
			var prec0 = operator != null ? operator.precedence() : -1;
			Operator op;
			Tree tree;
			while ((op = (tree = list.getLast()).getOperator()) != null) {
				var prec1 = op.precedence();
				if (prec0 < prec1 || operator.assoc() == Assoc.LEFT && prec0 == prec1)
					list.removeLast();
				else
					break;
			}
			return tree;
		}

		private void push(Tree tree) {
			list.addLast(tree);
			isDanglingRight = true;
		}
	}

	public Node parse(String in0) {
		var in = Preprocess.transform(PreprocessorFactory.create(operators), in0).t0;
		var stack = new ArrayDeque<Section>();

		Sink<Operator> addOperator = operator -> {
			var section = stack.peek();
			var tree = section.unwind(operator);
			var tree1 = Tree.of(operator, tree.getRight(), Atom.NIL);
			Tree.forceSetRight(tree, tree1);
			section.push(tree1);
		};

		Sink<Node> add = node -> {
			var section = stack.peek();
			if (!section.isDanglingRight)
				addOperator.sink(TermOp.TUPLE_);
			Tree.forceSetRight(section.list.getLast(), node);
			section.isDanglingRight = false;
		};

		var lex = new Lexer(operators, in);
		stack.push(new Section(' '));
		Token token;

		while ((token = lex.lex()) != null) {
			var operator = token.operator;
			var data = token.getData();
			var ch = data.charAt(0);

			if (operator != null && (operator != TermOp.BRACES || isSpecialBraces)) {
				addOperator.sink(operator);
				if (operator == TermOp.BRACES)
					stack.push(new Section('{'));
			} else if (ch == '(' || ch == '[' || ch == '{')
				stack.push(new Section(ch));
			else if (ch == ')' || ch == ']' || ch == '}') {
				var section = stack.pop();
				var kind = section.kind;

				if (kind == '(' && ch == ')' || kind == '[' && ch == ']' || kind == '{' && ch == '}') {
					var node = section.unwind(null).getRight();
					if (ch == ']')
						node = TreeTuple.of(Atom.of("["), node);
					else if (ch == '}')
						node = TreeTuple.of(Atom.of("{"), node);
					add.sink(node);
				} else
					Fail.t("cannot parse " + in);
			} else if (ch == '`')
				if (stack.peek().kind == ch) {
					var node = stack.pop().unwind(null).getRight();
					node = TreeTuple.of(Atom.of("`"), node);
					add.sink(node);
				} else
					stack.push(new Section(ch));
			else if (String_.isNotBlank(data))
				add.sink(terminalParser.parseTerminal(data));
		}

		return stack.size() == 1 ? stack.pop().unwind(null).getRight() : Fail.t("cannot parse " + in);
	}

}
