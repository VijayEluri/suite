package suite.os;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import suite.Constants;
import suite.inspect.Dump;
import suite.proxy.Intercept;
import suite.util.Array_;
import suite.util.FunUtil.Source;

public class LogUtil {

	private static int maxStackTraceLength = 99;
	private static ThreadLocal<String> prefix = ThreadLocal.withInitial(() -> "");

	private static Log_ suiteLog = new Log_() {
		private Log log0 = LogFactory.getLog("suite");

		public void info(String message) {
			log0.info(message);
		}

		public void warn(String message) {
			log0.warn(message);
		}

		public void error(Throwable th) {
			boolean isTrimmed = trimStackTrace(th);
			log0.error(prefix.get() + (isTrimmed ? "(Trimmed)" : ""), th);
		}

		public void fatal(Throwable th) {
			boolean isTrimmed = trimStackTrace(th);
			log0.fatal(prefix.get() + (isTrimmed ? "(Trimmed)" : ""), th);
		}
	};

	private interface Log_ {
		public void info(String message);

		public void warn(String message);

		public void error(Throwable th);

		public void fatal(Throwable th);
	}

	static {
		initLog4j(Level.INFO);
	}

	public static void initLog4j(Level level) {
		Path logDir = Constants.tmp("logs");

		PatternLayout layout = new PatternLayout("%d %-5p [%c{1}] %m%n");

		ConsoleAppender console = new ConsoleAppender(layout);
		console.setWriter(new PrintWriter(System.err));
		console.activateOptions();

		DailyRollingFileAppender file = new DailyRollingFileAppender();
		file.setFile(logDir.resolve("suite.log").toString());
		file.setDatePattern("'.'yyyyMMdd");
		file.setLayout(layout);
		file.activateOptions();

		Logger logger = Logger.getRootLogger();
		logger.setLevel(level);
		logger.removeAllAppenders();
		logger.addAppender(console);
		logger.addAppender(file);
	}

	public static <T> T duration(String m, Source<T> source) {
		Stopwatch<T> tr = Stopwatch.of(source);
		LogUtil.info(m + " in " + tr.duration + "ms, GC occurred " + tr.nGcs + " times in " + tr.gcDuration + " ms");
		return tr.result;
	}

	public static <T> T prefix(String s, Source<T> source) {
		String prefix0 = prefix.get();
		prefix.set(prefix0 + s);
		try {
			return source.source();
		} finally {
			prefix.set(prefix0);
		}
	}

	public static void info(String message) {
		suiteLog.info(prefix.get() + message);
	}

	public static void warn(String message) {
		suiteLog.warn(prefix.get() + message);
	}

	public static void error(Throwable th) {
		suiteLog.error(th);
	}

	public static void fatal(Throwable th) {
		suiteLog.fatal(th);
	}

	public static <T> T log(String message, Source<T> source) {
		info("Enter " + message);
		try {
			return source.source();
		} finally {
			info("Exit " + message);
		}
	}

	public static <I> I proxy(Class<I> interface_, I object) {
		Log log = LogFactory.getLog(object.getClass());

		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			String methodName = m.getName();
			String prefix = methodName + "()\n";
			StringBuilder sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (int i = 0; i < ps.length; i++)
					Dump.object(sb, "p" + i, ps[i]);

			log.info(sb.toString());

			try {
				Object value = invocation.invoke(m, ps);
				String rd = Dump.object("return", value);
				log.info(prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				Throwable th = ite.getTargetException();
				boolean isTrimmed = trimStackTrace(th);
				log.error(prefix + (isTrimmed ? "(Trimmed)" : ""), th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		});
	}

	private static boolean trimStackTrace(Throwable th) {
		boolean isTrimmed = false;

		// trims stack trace to appropriate length
		while (th != null) {
			StackTraceElement[] st0 = th.getStackTrace();

			if (maxStackTraceLength < st0.length) {
				StackTraceElement[] st1 = new StackTraceElement[maxStackTraceLength];
				Array_.copy(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

}
