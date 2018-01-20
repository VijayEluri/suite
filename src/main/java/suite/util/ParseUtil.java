package suite.util;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.text.Segment;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;

public class ParseUtil {

	public static String[] fit(String in, String... parts) {
		return fit(in, s -> s, parts);
	}

	public static String[] fitCaseInsensitive(String in, String... parts) {
		return fit(in, String::toLowerCase, parts);
	}

	public static String[] fit(String in, Iterate<String> lower, String... parts) {
		List<String> outs = new ArrayList<>();
		String inl = lower.apply(in);
		int p = 0;
		for (String part : parts) {
			int p1 = inl.indexOf(lower.apply(part), p);
			if (0 <= p1) {
				outs.add(in.substring(p, p1));
				p = p1 + part.length();
			} else
				return null;
		}
		outs.add(in.substring(p));
		return outs.toArray(new String[0]);
	}

	public static List<String> searchn(String s, String name, Assoc assoc) {
		List<String> list = new ArrayList<>();
		Pair<String, String> pair;

		while ((pair = search(s, name, assoc)) != null) {
			list.add(pair.t0);
			s = pair.t1;
		}

		list.add(s);
		return list;
	}

	public static int search(String s, int start, String toMatch) {
		int nameLength = toMatch.length();
		int end = s.length() - nameLength;
		int quote = 0;

		for (int pos = start; pos <= end; pos++) {
			char c = s.charAt(pos);
			quote = getQuoteChange(quote, c);

			if (quote == 0 && s.startsWith(toMatch, pos))
				return pos;
		}

		return -1;
	}

	public static Pair<String, String> search(String s, String name, Assoc assoc) {
		return search(s, Segment.of(0, s.length()), name, assoc, true);
	}

	private static Pair<String, String> search(String s, Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		Segment ops = searchPosition(s.toCharArray(), segment, name, assoc, isCheckDepth);

		if (ops != null) {
			String left = s.substring(segment.start, ops.start);
			String right = s.substring(ops.end, segment.end);
			return Pair.of(left, right);
		} else
			return null;
	}

	public static Streamlet<String> split(String in, String name) {
		char[] chars = in.toCharArray();
		int length = chars.length;

		return new Streamlet<>(() -> Outlet.of(new Source<String>() {
			private int pos = 0;

			public String source() {
				if (pos < length) {
					Segment segment = searchPosition(chars, Segment.of(pos, length), name, Assoc.LEFT, true);
					int pos0 = pos;
					int end;
					if (segment != null) {
						end = segment.start;
						pos = segment.end;
					} else
						end = pos = length;
					return new String(chars, pos0, end);
				} else
					return null;
			}
		}));
	}

	public static Segment searchPosition(char[] cs, Segment segment, Operator operator) {
		return searchPosition(cs, segment, operator.getName(), operator.getAssoc(), true);
	}

	public static Segment searchPosition(char[] cs, Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		int nameLength = name.length();
		int start1 = segment.start, end1 = segment.end - 1;
		int quote = 0, depth = 0;
		int pos0, posx, step;

		if (start1 <= end1) {
			if (assoc == Assoc.RIGHT) {
				pos0 = start1;
				posx = end1;
				step = 1;
			} else {
				pos0 = end1;
				posx = start1;
				step = -1;
			}

			for (int pos = pos0; pos != posx + step; pos += step) {
				char c = cs[pos];
				quote = getQuoteChange(quote, c);

				if (quote == 0) {
					if (isCheckDepth)
						depth = checkDepth(depth, c);

					if (depth == 0 && pos + nameLength <= cs.length) {
						boolean result = true; // cs.startsWith(name, pos)
						for (int i = 0; result && i < nameLength; i++)
							result &= cs[pos + i] == name.charAt(i);
						if (result)
							return Segment.of(pos, pos + nameLength);
					}
				}
			}
		}

		return null;
	}

	public static boolean isParseable(String s) {
		return isParseable(s, false);
	}

	/**
	 * Judges if the input string has balanced quote characters and bracket
	 * characters.
	 *
	 * @param isThrow
	 *            if this is set to true, and the string is deemed unparseable even
	 *            if more characters are added, throw exception.
	 */
	public static boolean isParseable(String s, boolean isThrow) {
		int quote = 0, depth = 0;

		// shows warning if the atom has mismatched quotes or brackets
		for (char c : String_.chars(s)) {
			quote = getQuoteChange(quote, c);
			if (quote == 0)
				depth = checkDepth(depth, c);
		}

		return !isThrow || 0 <= depth ? quote == 0 && depth == 0 : Fail.t("parse error");
	}

	private static int checkDepth(int depth, char c) {
		if (c == '(' || c == '[' || c == '{')
			depth++;
		if (c == ')' || c == ']' || c == '}')
			depth--;
		return depth;
	}

	public static int getQuoteChange(int quote, char c) {
		if (c == quote)
			quote = 0;
		else if (quote == 0 && (c == '\'' || c == '"' || c == '`'))
			quote = c;
		return quote;
	}

	public static boolean isWhitespace(byte b) {
		return b == 0;
	}

	public static boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}

	public static boolean isWhitespace(double d) {
		return d == 0d;
	}

	public static boolean isWhitespace(float f) {
		return f == 0f;
	}

	public static boolean isWhitespace(int i) {
		return i == 0;
	}

}
