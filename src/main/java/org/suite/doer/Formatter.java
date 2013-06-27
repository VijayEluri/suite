package org.suite.doer;

import java.util.HashSet;
import java.util.Set;

import org.parser.Operator;
import org.parser.Operator.Assoc;
import org.parser.Parser;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.util.Util;

public class Formatter {

	private boolean isDump;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	public Formatter(boolean isDump) {
		this.isDump = isDump;
	}

	public static String display(Node node) {
		return new Formatter(false).format(node);
	}

	public static String dump(Node node) {
		return new Formatter(true).format(node);
	}

	public static String treeize(Node node) {
		StringBuilder sb = new StringBuilder();
		treeize(node, sb, "");
		return sb.toString();
	}

	private static void treeize(Node node, StringBuilder sb, String indent) {
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			String op = tree.getOperator().getName();
			op = Util.equals(op, " ") ? "[]" : op.trim();
			String indent1 = indent + "  ";

			treeize(tree.getLeft(), sb, indent1);
			sb.append(indent + op + "\n");
			treeize(tree.getRight(), sb, indent1);
		} else
			sb.append(indent + dump(node) + "\n");
	}

	private String format(Node node) {
		format(node, 0);
		return sb.toString();
	}

	/**
	 * Converts a node to its string representation.
	 * 
	 * @param node
	 *            Node to be converted.
	 * @param parentPrec
	 *            Minimum operator precedence without adding parentheses.
	 */
	private void format(Node node, int parentPrec) {
		node = node.finalNode();
		Integer objectId = System.identityHashCode(node);

		// Avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format0(node, parentPrec);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format0(Node node, int parentPrec) {
		if (node instanceof Int)
			sb.append(((Int) node).getNumber());
		else if (node instanceof Atom) {
			String s = ((Atom) node).getName();
			s = isDump ? quoteAtomIfRequired(s) : s;
			sb.append(s);
		} else if (node instanceof Str) {
			String s = ((Str) node).getValue();
			s = isDump ? quote(s, '"') : s;
			sb.append(s);
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator operator = tree.getOperator();
			int ourPrec = operator.getPrecedence();
			Assoc assoc = operator.getAssoc();
			boolean isNeedParentheses = ourPrec <= parentPrec;
			int leftPrec = ourPrec - (assoc == Assoc.LEFT ? 1 : 0);
			int rightPrec = ourPrec - (assoc == Assoc.RIGHT ? 1 : 0);

			if (isNeedParentheses)
				sb.append('(');

			format(tree.getLeft(), leftPrec);

			if (operator != TermOp.BRACES) {
				String name = operator.getName();
				sb.append(name);

				if (operator != TermOp.AND___ || tree.getRight() != Atom.NIL) {
					if (operator == TermOp.AND___ || operator == TermOp.OR____)
						sb.append(' ');

					format(tree.getRight(), rightPrec);
				} // a, () suppressed as a,
			} else {
				sb.append(" {");
				format(tree.getRight(), 0);
				sb.append("}");
			}

			if (isNeedParentheses)
				sb.append(')');
		} else if (node instanceof Reference)
			sb.append(Generalizer.defaultPrefix + ((Reference) node).getId());
		else
			sb.append(node.getClass().getSimpleName() + '@' + Integer.toHexString(node.hashCode()));
	}

	public String quoteAtomIfRequired(String s) {
		if (!s.isEmpty()) {
			boolean quote = false;

			for (char c : s.toCharArray())
				quote |= !('0' <= c && c <= '9') //
						&& !('a' <= c && c <= 'z') //
						&& !('A' <= c && c <= 'Z') //
						&& c != '.' && c != '-' && c != '_' && c != '$';

			quote |= s.contains(Parser.closeGroupComment) //
					|| s.contains(Parser.openGroupComment) //
					|| s.contains(Parser.closeLineComment) //
					|| s.contains(Parser.openLineComment);

			try {
				Integer.parseInt(s);
				quote |= true;
			} catch (Exception ignored) {
			}

			if (quote)
				s = quote(s, '\'');
		} else
			s = "()";
		return s;
	}

	public String quote(String s, char quote) {
		StringBuilder sb = new StringBuilder();
		sb.append(quote);

		for (char ch : s.toCharArray())
			if (Character.isWhitespace(ch) && ch != ' ') {
				if (ch >= 256)
					sb.append(encodeHex(ch >> 8));
				sb.append(encodeHex(ch & 0xff));
			} else if (ch == quote || ch == '%')
				sb.append(ch + "" + ch);
			else
				sb.append(ch);

		sb.append(quote);
		return sb.toString();
	}

	private String encodeHex(int i) {
		return "%" + String.format("%02x", i).toUpperCase();
	}

}
