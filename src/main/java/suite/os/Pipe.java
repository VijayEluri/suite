package suite.os;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import suite.Constants;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.Rethrow;

public class Pipe {

	public static Streamlet<String> shell(String sh) {
		String[] command0 = null;

		for (var s : List.of("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe"))
			if (Files.exists(Paths.get(s)))
				command0 = new String[] { s, };

		if (command0 != null)
			LogUtil.info("START " + sh);
		else
			Fail.t("cannot find shell executable");

		var command1 = command0;

		return new Streamlet<>(() -> Rethrow.ex(() -> {
			var bis = new ByteArrayInputStream(sh.getBytes(Constants.charset));

			var process = Rethrow.ex(() -> Runtime.getRuntime().exec(command1));

			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			var threads = new Thread[] { //
					Copy.streamByThread(pes, System.err), //
					Copy.streamByThread(bis, pos), };

			for (var thread : threads)
				thread.start();

			return Read.lines(pis).closeAtEnd(() -> {
				try {
					var code = process.waitFor();

					if (code == 0)
						for (var thread : threads)
							thread.join();
					else
						Fail.t("code = " + code);
				} catch (InterruptedException ex) {
					Fail.t(ex);
				}
				process.destroy();
				LogUtil.info("END__ " + sh);
			});
		}));
	}

}
