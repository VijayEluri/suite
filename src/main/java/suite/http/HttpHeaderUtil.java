package suite.http;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.Constants;
import suite.immutable.IList;
import suite.util.Rethrow;
import suite.util.String_;

public class HttpHeaderUtil {

	public static IList<String> getPath(String pathString) {
		var arr = pathString.split("/");
		var path = IList.<String> end();
		for (var i = arr.length - 1; i >= 0; i--)
			if (!arr[i].isEmpty())
				path = IList.cons(arr[i], path);
		return path;
	}

	public static Map<String, String> getCookieAttrs(String query) {
		return decodeMap(query, ";");
	}

	public static Map<String, String> getPostedAttrs(InputStream is) {
		var reader = new InputStreamReader(is, Constants.charset);
		var sb = new StringBuilder();
		var buffer = new char[Constants.bufferSize];
		int nCharsRead;

		while (0 <= (nCharsRead = Rethrow.ex(() -> reader.read(buffer))))
			sb.append(buffer, 0, nCharsRead);

		return getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) {
		return decodeMap(query, "&");
	}

	private static Map<String, String> decodeMap(String query, String sep) {
		var qs = query != null ? query.split(sep) : new String[0];
		var attrs = new HashMap<String, String>();
		for (var q : qs)
			String_.split2l(q, "=").map((k, v) -> attrs.put(k, decode(v)));
		return attrs;
	}

	private static String decode(String s) {
		return Rethrow.ex(() -> URLDecoder.decode(s, Constants.charset));
	}

}
