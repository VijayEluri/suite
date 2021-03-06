package suite.trade.data;

import static suite.util.Friends.forInt;

import suite.http.HttpUtil;
import suite.primitive.Chars_;

public class Aastocks {

	public float hsi() {
		var url = "http://www.aastocks.com/en/mobile/Quote.aspx?symbol=00005";
		var lines = HttpUtil.get(url).lines().toList();
		var i0 = forInt(lines.size()).filter(i -> lines.get(i).contains("HSI")).first();
		String s = lines.get(i0 + 1);

		// remove all quoted strings
		while (true) {
			var p0 = s.indexOf('"');
			var p1 = 0 <= p0 ? s.indexOf('"', p0 + 1) : -1;
			if (0 <= p1)
				s = s.substring(0, p0 - 1) + s.substring(p1 + 1);
			else
				break;
		}

		// extract the number we want
		return Float.parseFloat(new String(Chars_.of(s.toCharArray()) //
				.filter(c -> c == '.' || '0' <= c && c <= '9') //
				.toArray()));
	}

}
