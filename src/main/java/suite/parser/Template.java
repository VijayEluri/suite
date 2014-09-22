package suite.parser;

import suite.util.FunUtil.Fun;

public class Template {

	private static String open = "<%";
	private static String close = "%>";

	public String render(String in, Fun<String, String> fun) {
		StringBuilder sb = new StringBuilder();
		int start = 0;

		while (true) {
			int pos0 = in.indexOf(open, start);
			if (pos0 == -1)
				break;
			int pos1 = in.indexOf(close, pos0 + open.length());
			if (pos1 == -1)
				break;

			sb.append(fun.apply(in.substring(start, pos0)));
			sb.append(in.substring(start, pos1));
			start = pos1 + close.length();
		}

		sb.append(fun.apply(in.substring(start)));
		return sb.toString();
	}

}
