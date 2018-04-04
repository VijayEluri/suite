package suite.jdk;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

import suite.Constants;
import suite.os.FileUtil;

public class JdkUtilTest {

	@Test
	public void test() throws IOException {
		Path srcDir = Constants.tmp("src");
		Path binDir = Constants.tmp("bin");
		var className = "HelloWorld";

		FileUtil.mkdir(srcDir);
		FileUtil.mkdir(binDir);

		var src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (JdkLoadClassUtil jdkLoadClassUtil = new JdkLoadClassUtil(srcDir, binDir)) {
			jdkLoadClassUtil.newInstance(Runnable.class, className, src).run();
		}

		new JdkUnsafeLoadClassUtil(srcDir, binDir).newInstance(Runnable.class, className, src).run();
	}

}
