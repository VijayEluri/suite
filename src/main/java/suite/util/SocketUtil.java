package suite.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

public class SocketUtil {

	public interface Io {
		public void serve(InputStream is, OutputStream os) throws IOException;
	}

	public interface Rw {
		public void serve(Reader reader, PrintWriter writer) throws IOException;
	}

	public static void listen(int port, Rw rw) throws IOException {
		listen(port, (InputStream is, OutputStream os) -> {
			try (Reader reader = new BufferedReader(new InputStreamReader(is)); PrintWriter writer = new PrintWriter(os)) {
				rw.serve(reader, writer);
			}
		});
	}

	public static void listen(int port, Io io) throws IOException {
		ThreadPoolExecutor executor = Util.createExecutor();

		try (ServerSocket server = new ServerSocket(port)) {
			while (true) {
				Socket socket = server.accept();

				executor.execute(() -> {
					try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
						io.serve(is, os);
					} catch (Exception ex) {
						LogUtil.error(ex);
					} finally {
						Util.closeQuietly(socket);
					}
				});
			}
		} finally {
			executor.shutdown();
		}
	}

}
