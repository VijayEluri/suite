package suite.parser;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.As;
import suite.streamlet.Read;

public class RenderFunctionalTemplateTest {

	@Test
	public void test() {
		var fruits = Arrays.<Node> asList(new Str("orange"), new Str("apple"), new Str("pear"));

		var map = Read //
				.from2(List.of( //
						Pair.of("list", Tree.of(TermOp.OR____, fruits)), //
						Pair.of("title", new Str("My favourite things"))) //
				) //
				.collect(As::map);

		System.out.println(new RenderFunctionalTemplate() //
				.render("" //
						+ "<html> \n" //
						+ "    <head> \n" //
						+ "        <#= title #> \n" //
						+ "    </head> \n" //
						+ "    </body> \n" //
						+ "        Fruits: \n" //
						+ "<# . (list | apply . map {fruit => id#>        <li><#= fruit #></li> \n" //
						+ "<#}) #>    <body> \n" //
						+ "</html> \n", //
						map));
	}

}