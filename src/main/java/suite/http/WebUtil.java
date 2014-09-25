package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.util.FunUtil.Source;
import suite.util.To;

public class WebUtil {

	public static class HttpResult {
		public int responseCode;
		public Source<Bytes> out;

		private HttpResult(int responseCode, Source<Bytes> out) {
			this.responseCode = responseCode;
			this.out = out;
		}
	}

	public static HttpResult http(URL url) throws IOException {
		return http("GET", url);
	}

	public static HttpResult http(String method, URL url) throws IOException {
		return http(method, url, () -> null);
	}

	public static HttpResult http(String method, URL url, Source<Bytes> in) throws IOException {
		return http(method, url, in, Collections.emptyMap());
	}

	public static HttpResult http(String method, URL url, Source<Bytes> in, Map<String, String> headers) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		headers.entrySet().forEach(e -> conn.setRequestProperty(e.getKey(), e.getValue()));

		try (OutputStream os = conn.getOutputStream()) {
			BytesUtil.copy(in, os);
		}

		int responseCode = conn.getResponseCode();
		if (responseCode == 200)
			return new HttpResult(responseCode, To.source(conn.getInputStream()));
		else
			throw new IOException("HTTP returned " + responseCode + ":" + url);
	}

}
