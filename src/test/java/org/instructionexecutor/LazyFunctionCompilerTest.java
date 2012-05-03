package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;

public class LazyFunctionCompilerTest {

	@Test
	public void testClosure() {
		assertEquals(SuiteUtil.parse("4") //
				, eval("define v as number = 4 >> (i => j => v) {1} {2}"));
	}

	@Test
	public void testCorecursion() {
		assertEquals(Atom.create("true"), eval("" //
				+ "define seq = (n => n, seq {n}) >> \n" //
				+ "head {seq {0}} = 0"));

		assertEquals(Int.create(89), eval("" // Real co-recursion!
				+ "define fib = (i1 => i2 => i2, fib {i2} {i1 + i2}) >> \n" //
				+ "define t = (f => tail {f}) >> \n" //
				+ "(head . apply {fib {0} {1}} . repeat {10} | t)"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(89), eval("" //
				+ "define fib = ( \n" //
				+ "    1, 1, zip {`+`} {fib} {tail {fib}} \n" //
				+ ") >> \n" //
				+ "define t = (f => tail {f}) >> \n" //
				+ "(head . apply {fib} . repeat {10} | t)"));
	}

	@Test
	public void testGet() {
		assertEquals(Int.create(3), eval("get {2} {1:2:3:4:}"));
	}

	@Test
	public void testProve() {
		assertEquals(Atom.create("true"), eval("" //
				+ "prove-tf () is.atom abc"));
		assertEquals(Atom.create("true"), eval("" //
				+ "prove-tf _v:3/ (_v = 3)"));
		assertEquals(Atom.create("false"), eval("" //
				+ "prove-tf _v:4/ (_v = 3)"));
		assertEquals(Int.create(4), eval("" //
				+ "prove-r _v:2/ (let _r (2 * _v)) _r"));
	}

	@Test
	public void testString() {
		assertEquals(Int.create(-34253924), eval("str-to-int {\"-34253924\"}"));
		assertEquals(Atom.create("true"),
				eval("equals {\"-34253924\"} {int-to-str {-34253924}}"));
	}

	@Test
	public void testSubstitution() {
		assertEquals(Int.create(8), eval("define v = 4 >> v + v"));
	}

	@Test
	public void testSystem() {
		assertEquals(Atom.create("true"), eval("1 = 1"));
		assertEquals(Atom.create("false"), eval("1 = 2"));
		eval("cons {1} {}");
		eval("head {1, 2, 3,}");
		eval("tail {1, 2, 3,}");
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateLazyFunctional(f);
	}

}
