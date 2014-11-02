package suite.parser;

import suite.util.FunUtil.Fun;

/**
 * Render template into pages.
 *
 * @author ywsing
 */
public class TemplatePreprocessor implements Fun<String, String> {

	public static String openTemplate = "<<";
	public static String closeTemplate = ">>";

	private Fun<String, String> wrapText;
	private Fun<String, String> wrapExpression;

	public TemplatePreprocessor(Fun<String, String> wrapText, Fun<String, String> wrapExpression) {
		this.wrapText = wrapText;
		this.wrapExpression = wrapExpression;
	}

	@Override
	public String apply(String in) {
		int start = 0;
		StringBuilder sb = new StringBuilder();

		while (true) {
			int pos0 = in.indexOf(openTemplate, start);
			if (pos0 == -1)
				break;
			int pos1 = pos0 + openTemplate.length();

			int pos2 = in.indexOf(closeTemplate, pos1);
			if (pos2 == -1)
				break;
			int pos3 = pos2 + closeTemplate.length();

			sb.append(wrapText.apply(in.substring(start, pos0)));
			sb.append(wrapExpression.apply(in.substring(pos1, pos2)));
			start = pos3;
		}

		sb.append(wrapText.apply(in.substring(start)));
		return sb.toString();
	}

}
