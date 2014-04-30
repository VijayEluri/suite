package suite.parser;

import java.util.Map;

import suite.util.FunUtil.Fun;

public class Subst {

	private String openSubst;
	private String closeSubst;

	public Subst() {
		this("${", "}");
	}

	public Subst(String openSubst, String closeSubst) {
		this.openSubst = openSubst;
		this.closeSubst = closeSubst;
	}

	public String subst(String s, Map<String, String> map) {
		return subst(s, new Fun<String, String>() {
			public String apply(String key) {
				return map.get(key);
			}
		});
	}

	public String subst(String s, Fun<String, String> fun) {
		StringBuilder sb = new StringBuilder();
		subst(s, fun, sb);
		return sb.toString();
	}

	public void subst(String s, Fun<String, String> fun, StringBuilder sb) {
		while (true) {
			int pos0 = s.indexOf(openSubst);
			int pos1 = s.indexOf(closeSubst, pos0);

			if (pos0 >= 0 && pos1 >= 0) {
				String left = s.substring(0, pos0);
				String key = s.substring(pos0 + 2, pos1);
				String right = s.substring(pos1 + 1);
				String value = fun.apply(key);

				if (value != null) {
					sb.append(left);
					subst(value, fun, sb);
					s = right;
					continue;
				}
			}

			sb.append(s);
			break;
		}
	}

}
