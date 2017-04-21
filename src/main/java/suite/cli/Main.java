package suite.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.Constants;
import suite.Suite;
import suite.net.SocketServer;
import suite.node.Node;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.util.FunUtil.Source;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 *
 * @author ywsing
 */
// mvn compile exec:java -Dexec.mainClass=suite.cli.Main
public class Main extends ExecutableProgram {

	private CommandOptions opt;
	private CommandDispatcher dispatcher;

	private Reader reader = new BufferedReader(new InputStreamReader(System.in, Constants.charset));
	private Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, Constants.charset));

	public static void main(String[] args) {
		Util.run(Main.class, args);
	}

	protected boolean run(String[] args) throws IOException {
		opt = new CommandOptions();

		boolean result = true;
		List<String> inputs = new ArrayList<>();
		Source<String> source = To.source(args);
		String verb = null;
		String arg;

		while ((arg = source.source()) != null)
			if (arg.startsWith("-"))
				if (Util.stringEquals(arg, "--file"))
					inputs.add(readScript(source.source()));
				else
					result &= opt.processOption(arg, source);
			else if (verb == null)
				verb = arg;
			else
				inputs.add(arg);

		dispatcher = new CommandDispatcher(opt);

		if (result)
			if (Util.stringEquals(verb, "evaluate"))
				result &= dispatcher.dispatchEvaluate(inputs);
			else if (Util.stringEquals(verb, "filter"))
				result &= dispatcher.dispatchFilter(inputs, reader, writer);
			else if (Util.stringEquals(verb, "precompile"))
				result &= dispatcher.dispatchPrecompile(inputs);
			else if (Util.stringEquals(verb, "prove"))
				result &= dispatcher.dispatchProve(inputs);
			else if (Util.stringEquals(verb, "query"))
				result &= runInteractive(inputs);
			else if (Util.stringEquals(verb, "serve"))
				new SocketServer().run();
			else if (Util.stringEquals(verb, "type"))
				result &= dispatcher.dispatchType(inputs);
			else if (verb == null)
				result &= runInteractive(inputs);
			else
				throw new RuntimeException("Unknown action " + verb);

		return result;
	}

	private String readScript(String filename) throws IOException {
		String contents = FileUtil.read(filename);
		if (contents.startsWith("#")) // skips first line comment
			contents = contents.substring(contents.indexOf('\n') + 1);
		return contents;
	}

	private boolean runInteractive(List<String> filenames) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		boolean code = true;
		String ready;

		code &= dispatcher.importFiles(filenames);

		try (Writer sw = new StringWriter()) {
			Node node = Suite.applyWriter(Suite.parse("\"READY\""));
			Suite.evaluateFunToWriter(opt.fcc(node), sw);
			ready = sw.toString();
		} catch (Exception ex) {
			LogUtil.error(ex);
			ready = "ERROR";
		}

		opt.prompt().println(ready);

		while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					opt.prompt().print(sb.length() == 0 ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						return code;
				} while (!opt.isQuiet() //
						&& (!ParseUtil.isParseable(sb.toString(), true) || !line.isEmpty() && !line.endsWith("#")));

				code &= dispatcher.dispatchCommand(sb.toString(), writer);
			} catch (Throwable ex) {
				LogUtil.error(ex);
			}
	}

	@Override
	public void close() {
		Util.closeQuietly(reader);
		Util.closeQuietly(writer);
	}

}
