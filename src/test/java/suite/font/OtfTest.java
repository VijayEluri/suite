package suite.font;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import suite.os.Execute;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Object_;

public class OtfTest {

	@Test
	public void otfTest() {
		var familyKey = "Family";
		var subfamilyKey = "Subfamily";
		var keys = List.of(familyKey, subfamilyKey);

		var commands = Read //
				.each("/tmp/fonts") //
				.map(Paths::get) //
				.concatMap(FileUtil::findPaths) //
				.map(Path::toString) //
				.filter(path -> {
					var pathl = path.toLowerCase();
					return pathl.endsWith(".otf") || pathl.endsWith(".ttf");
				}) //
				.map2(path -> {
					var exec = new Execute(new String[] { "otfinfo", "-i", path, });
					return Read //
							.from(exec.out.split("\n")) //
							.map(line -> line.split(":")) //
							.filter(arr -> 2 <= arr.length) //
							.map2(arr -> arr[0].trim(), arr -> arr[1].trim()) //
							.filterKey(keys::contains) //
							.toMap();
				}) //
				.map((k, m) -> {
					var f = m.get(familyKey);
					var sf = m.get(subfamilyKey);
					var dir = "/home/ywsing/.fonts/" + f + "/";
					var ext = k.substring(k.lastIndexOf(".") + 1).toLowerCase();

					return "mkdir -p '" + dir + "'; mv '" + k + "' '" + dir + f + " " + sf + "." + ext + "'";
				}) //
				.sort(Object_::compare) //
				.toList();

		for (var command : commands)
			System.out.println(command);
	}

}
