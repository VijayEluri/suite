package suite.parser;

import suite.streamlet.FunUtil.Iterate;

public class Template {

	private static String open = "<%";
	private static String close = "%>";

	public String render(String in, Iterate<String> fun) {
		var sb = new StringBuilder();
		var pos0 = 0;
		int pos1, pos2;

		while (0 <= (pos1 = in.indexOf(open, pos0)) //
				&& 0 <= (pos2 = in.indexOf(close, pos1 + open.length()))) {
			sb.append(fun.apply(in.substring(pos0, pos1)));
			sb.append(in.substring(pos0, pos2));
			pos0 = pos2 + close.length();
		}

		sb.append(fun.apply(in.substring(pos0)));
		return sb.toString();
	}

}
