package org.suite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.node.Tree;

public class ParserTest {

	@Test
	public void testParse() {
		assertNotNull(Tree.decompose(Suite.parse("!, a")).getLeft());
		assertEquals(Suite.parse("{0}"), Suite.parse("`{0}`"));
	}

}
