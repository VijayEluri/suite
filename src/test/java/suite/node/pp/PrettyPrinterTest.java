package suite.node.pp;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.os.FileUtil;

public class PrettyPrinterTest {

	private PrettyPrinter prettyPrinter = new PrettyPrinter();

	@Test
	public void test() throws IOException {
		System.out.println(prettyPrinter.prettyPrint(Suite.parse(FileUtil.read("src/main/ll/fc/fc.sl"))));
	}

}
