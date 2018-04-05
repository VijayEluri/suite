package suite.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import suite.Constants;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.To;

public class Execute {

	public final int code;
	public final String out;
	public final String err;
	private Thread[] threads;

	public static String shell(String sh) {
		String[] command = null;

		for (var s : List.of("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe"))
			if (Files.exists(Paths.get(s)))
				command = new String[] { s, };

		if (command != null) {
			LogUtil.info("START " + sh);
			var execute = new Execute(command, sh);
			LogUtil.info("END__ " + sh);
			if (execute.code == 0)
				return execute.out;
			else
				return Fail.t(execute.toString());
		} else
			return Fail.t("cannot find shell executable");
	}

	public Execute(String[] command) {
		this(command, "");
	}

	public Execute(String[] command, String in) {
		this(command, in.getBytes(Constants.charset));
	}

	public Execute(String[] command, byte[] bytes) {
		var bis = new ByteArrayInputStream(bytes);
		var bos0 = new ByteArrayOutputStream();
		var bos1 = new ByteArrayOutputStream();

		Process process = Rethrow.ex(() -> Runtime.getRuntime().exec(command));

		try {
			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			threads = new Thread[] { //
					Copy.streamByThread(pis, bos0), //
					Copy.streamByThread(pes, bos1), //
					Copy.streamByThread(bis, pos), };

			for (var thread : threads)
				thread.start();

			code = process.waitFor();

			for (var thread : threads)
				thread.join();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		} finally {
			process.destroy();
		}

		out = To.string(bos0.toByteArray());
		err = To.string(bos1.toByteArray());
	}

	@Override
	public String toString() {
		return "code = " + code //
				+ "\nout = " + out //
				+ "\nerr = " + err;
	}

}
