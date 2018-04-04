package suite.util;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class XmlUtilTest {

	@Test
	public void test() throws Exception {
		var xml0 = To.string(Files.readAllBytes(Paths.get("pom.xml")));
		var xml1 = new XmlUtil().format(xml0);
		System.out.println(xml1);
	}

}
