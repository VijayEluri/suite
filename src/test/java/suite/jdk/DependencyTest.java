package suite.jdk;

import static suite.util.Friends.rethrow;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import suite.object.Object_;
import suite.os.FileUtil;
import suite.persistent.PerSet;
import suite.search.DirectedGraph;
import suite.search.StronglyConnectedComponents;
import suite.streamlet.Read;

public class DependencyTest {

	private String dir = FileUtil.homeDir();
	private List<String> sourceDirs = List.of(dir + "/src/main/java", dir + "/src/test/java");

	@Test
	public void testDependency() {
		dumpDependencies(PerSet.empty(), "", "suite.util.Rethrow");
	}

	@Test
	public void testStronglyConnectedComponents() {
		var classes = new ArrayDeque<>(List.of("suite.jdk.DependencyTest"));
		var dependenciesByClassName = new HashMap<String, List<String>>();
		String className;

		while ((className = classes.pollLast()) != null) {
			var dependencies = getDependencies(className);
			dependenciesByClassName.put(className, dependencies);
			for (var className1 : dependencies)
				if (!dependenciesByClassName.containsKey(className1))
					classes.addLast(className1);
		}

		var vertices = dependenciesByClassName.keySet();

		var edges = Read //
				.from2(dependenciesByClassName) //
				.concatMapValue(dependencies -> Read.from(dependencies)) //
				.toSet();

		var scc = new StronglyConnectedComponents<>(DirectedGraph.of(vertices, edges));

		for (var layer : scc.group().layers()) {
			Read.from(layer).flatMap(iterable -> iterable).sort(Object_::compare).forEach(System.out::println);
			System.out.println();
		}
	}

	private void dumpDependencies(PerSet<String> set, String indent, String className) {
		if (!set.contains(className)) {
			System.out.println(indent + className);

			var set1 = set.add(className);
			getDependencies(className).forEach(className1 -> dumpDependencies(set1, indent + "  ", className1));
		}
	}

	private List<String> getDependencies(String className) {
		var p = className.replace('.', '/') + ".java";

		return Read //
				.from(sourceDirs) //
				.map(sourceDir -> Paths.get(sourceDir + "/" + p)) //
				.filter(path -> Files.exists(path)) //
				.concatMap(path -> rethrow(() -> Read.from(Files.readAllLines(path)))) //
				.filter(line -> line.startsWith("import ")) //
				.map(line -> line.split(" ")[1].replace(";", "")) //
				.toList();
	}

}
