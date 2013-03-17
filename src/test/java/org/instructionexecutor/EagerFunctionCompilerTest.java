package org.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.SuiteUtil.FunCompilerConfig;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;

public class EagerFunctionCompilerTest {

	@Test
	public void testAppend() {
		assertEquals(SuiteUtil.parse("1, 2, 3, 4, 5, 6, 7, 8,"), eval("" //
				+ "append {1, 2, 3, 4,} {5, 6, 7, 8,}"));
	}

	@Test
	public void testApply() {
		assertEquals(Int.create(2), eval("" //
				+ "apply {(a => 2),} {1}"));
		assertEquals(Int.create(2), eval("" //
				+ "apply {`+ 1`, `* 2`, `/ 5`,} {4}"));
	}

	@Test
	public void testClosure() {
		assertEquals(Int.create(7), eval("" //
				+ "define add = `+` >> add {3} {4}"));
		assertEquals(Int.create(20), eval("" //
				+ "define p = `+ 1` >> \n" //
				+ "define q = (n => p {n} * 2) >> \n" //
				+ "q {9}"));
	}

	@Test
	public void testConcat() {
		assertEquals(SuiteUtil.parse("1, 2, 3, 4, 5, 6,"), eval("" //
				+ "concat {(1, 2,), (3, 4,), (5, 6,),}"));
	}

	@Test
	public void testContains() {
		assertEquals(Atom.create("true"), eval("" //
				+ "contains {8, 9,} {7, 8, 9, 10, 11,}"));
		assertEquals(Atom.create("false"), eval("" //
				+ "contains {11, 12,} {7, 8, 9, 10, 11,}"));
	}

	@Test
	public void testCross() {
		assertEquals(SuiteUtil.parse("" //
				+ "((7, 1,), (7, 2,),), " //
				+ "((8, 1,), (8, 2,),), " //
				+ "((9, 1,), (9, 2,),),") //
				, eval("cross {a => b => a, b,} {7, 8, 9,} {1, 2,}"));

		assertEquals(Atom.create("true"), eval("" //
				+ "let list1 as list-of one-of (A, B, C,) \n" //
				+ "    = (A, B, C,) >> \n" //
				+ "let result = ( \n" //
				+ "    (A:1:, A:2:,), \n" //
				+ "    (B:1:, B:2:,), \n" //
				+ "    (C:1:, C:2:,), \n" //
				+ ") >> \n" //
				+ "cross {a => b => a:b:} {list1} {1, 2,} = result"));
	}

	@Test
	public void testEndsWith() {
		assertEquals(Atom.create("true"),
				eval("ends-with {1, 2, 3,} {4, 5, 6, 1, 2, 3,}"));
		assertEquals(Atom.create("false"),
				eval("ends-with {1, 2, 3,} {4, 5, 3, 1, 2, 6,}"));
	}

	@Test
	public void testEquals() {
		assertEquals(Atom.create("true"), eval("" //
				+ "equals {} {}"));
		assertEquals(Atom.create("false"), eval("" //
				+ "equals {1, 2,} {1, 3,}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(89), eval("" //
				+ "define fib = (n => \n" //
				+ "    if:: n > 1 \n" //
				+ "    then:: fib {n - 1} + fib {n - 2} \n" //
				+ "    else:: 1 \n" //
				+ ") >> \n" //
				+ "fib {10}"));
	}

	@Test
	public void testFilter() {
		assertEquals(SuiteUtil.parse("4, 6,"), eval("" //
				+ "filter {n => n % 2 = 0} {3, 4, 5, 6,}"));
	}

	@Test
	public void testFlip() {
		assertEquals(Int.create(2), eval("" //
				+ "flip {`-`} {3} {5}"));
	}

	@Test
	public void testFold() {
		assertEquals(Int.create(324), eval("" //
				+ "fold {`*`} {2, 3, 6, 9,}"));
		assertEquals(Int.create(79), eval("" //
				+ "fold-left {`-`} {100} {6, 7, 8,}"));
		assertEquals(Int.create(-93), eval("" //
				+ "fold-right {`-`} {100} {6, 7, 8,}"));
	}

	@Test
	public void testGcd() {
		Node f = SuiteUtil.parse("gcd {6} {9}");
		FunCompilerConfig c = SuiteUtil.fcc(f);
		c.addLibrary("MATH");
		assertEquals(Int.create(3), SuiteUtil.evaluateFunctional(c));
	}

	@Test
	public void testGet() {
		assertEquals(Int.create(3), eval("get {2} {1:2:3:4:}"));
	}

	@Test
	public void testIf() {
		assertEquals(Int.create(0), eval("" //
				+ "if (3 > 4) then 1 else 0"));
		assertEquals(Int.create(1), eval("" //
				+ "if (3 = 3) then 1 else 0"));
		assertEquals(Int.create(1), eval("" //
				+ "if (1 = 2) then 0 else-if (2 = 2) then 1 else 2"));
	}

	@Test
	public void testIfBind() {
		assertEquals(Int.create(1), eval("" //
				+ "if-bind (1 = 1) then 1 else 0"));

		assertEquals(Int.create(1), eval("" //
				+ "if-bind ((1, 2,) = (1, 2,)) then 1 else 0"));
		assertEquals(Int.create(0), eval(""
				+ "if-bind ((1, 2,) = (2, 2,)) then 1 else 0"));

		assertEquals(Int.create(1), eval("" //
				+ "let v = (1, 2,) >> if-bind (v = (1, 2,)) then 1 else 0"));
		assertEquals(Int.create(0), eval(""
				+ "let v = (1, 2,) >> if-bind (v = (1, 3,)) then 1 else 0"));

		assertEquals(Int.create(0), eval("" //
				+ "let v = true:1:2: >> \n"
				+ "if-bind (v = true:\\i:3:) then i else 0"));
		assertEquals(Int.create(1), eval("" //
				+ "let v = true:1:2: >> \n"
				+ "if-bind (v = true:\\i:2:) then i else 0"));
		assertEquals(Int.create(1), eval("" //
				+ "if-bind (1:2: = \\i:2:) then i else 0"));

		assertEquals(Int.create(3), eval("" //
				+ "define type t = one-of (A, B number, C boolean,) >> \n" //
				+ "let e = B 3 >> \n" //
				+ "if-bind (e = B \\i) then i else 0"));
		assertEquals(Int.create(0), eval("" //
				+ "define type t = one-of (A, B number, C boolean,) >> \n" //
				+ "let e = B 3 >> \n" //
				+ "let f = C false >> \n" //
				+ "if-bind (e = f) then 1 else 0"));
	}

	@Test
	public void testInfiniteLoop() {
		try {
			// This would fail stack over during type check, so skip that
			SuiteUtil.evaluateEagerFunctional("no-type-check" //
					+ " (e => e {e}) {e => e {e}}");
			throw new RuntimeException();
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
	}

	@Test
	public void testJoin() {
		assertEquals(Int.create(19), eval("" //
				+ "define p = (`* 2`) >> \n" //
				+ "define q = (`+ 1`) >> \n" //
				+ "define r = (q . p) >> \n" //
				+ "r {9}"));
		assertEquals(Int.create(13), eval("" //
				+ "define p = (`+ 1`) >> \n" //
				+ "define q = (`* 2`) >> \n" //
				+ "define r = (`- 3`) >> \n" //
				+ "(p . q . r) {9}"));
		assertEquals(Int.create(17), eval("" //
				+ "define p = (`+ 1`) >> \n" //
				+ "define q = (`* 2`) >> \n" //
				+ "define r = (`- 3`) >> \n" //
				+ "9 | p | q | r"));
	}

	@Test
	public void testLength() {
		assertEquals(Int.create(5), eval("" //
				+ "length {3, 3, 3, 3, 3,}"));
	}

	@Test
	public void testLog() {
		assertEquals(Int.create(8), eval("" //
				+ "log {4 + 4}"));
		assertEquals(Int.create(1), eval("" //
				+ "if (1 = 1) then 1 else (1 / 0)"));
		assertEquals(Int.create(1), eval("" //
				+ "if true then 1 else (log2 {\"shouldn't appear\"} {1 / 0})"));
		assertEquals(Int.create(1), eval("" //
				+ "if false then 1 else (log2 {\"should appear\"} {1})"));
	}

	@Test
	public void testMap() {
		assertEquals(SuiteUtil.parse("5, 6, 7,"), eval("" //
				+ "map {`+ 2`} {3, 4, 5,}"));
	}

	@Test
	public void testMergeSort() {
		assertEquals(SuiteUtil.parse("0, 1, 2, 3, 4, 5, 6, 7, 8, 9,"), eval("" //
				+ "merge-sort {merge} {5, 3, 2, 8, 6, 4, 1, 0, 9, 7,}"));
	}

	@Test
	public void testOperator() {
		assertEquals(Atom.create("true"), eval("" //
				+ "and {1 = 1} {or {1 = 0} {1 = 1}}"));
		assertEquals(Atom.create("false"), SuiteUtil.evaluateEagerFunctional("" //
				+ "let list1 as list-of one-of (A, B,) = () >> A = B"));
	}

	@Test
	public void testQuickSort() {
		assertEquals(SuiteUtil.parse("0, 1, 2, 3, 4, 5, 6, 7, 8, 9,"), eval("" //
				+ "quick-sort {`<`} {5, 3, 2, 8, 6, 4, 1, 0, 9, 7,}"));
	}

	@Test
	public void testRange() {
		assertEquals(SuiteUtil.parse("2, 5, 8, 11,"), eval("" //
				+ "range {2} {14} {3}"));
	}

	@Test
	public void testRepeat() {
		assertEquals(SuiteUtil.parse("3, 3, 3, 3,"), eval("repeat {4} {3}"));
	}

	@Test
	public void testReverse() {
		assertEquals(SuiteUtil.parse("5, 4, 3, 2, 1,"),
				eval("reverse {1, 2, 3, 4, 5,}"));
	}

	@Test
	public void testStartsWith() {
		assertEquals(Atom.create("true"),
				eval("starts-with {1, 2, 3,} {1, 2, 3, 4, 5, 6,}"));
		assertEquals(Atom.create("false"),
				eval("starts-with {1, 2, 3,} {1, 2, 4, 3, 5, 6,}"));
	}

	@Test
	public void testSubstring() {
		assertEquals(eval("\"abcdefghij\""), eval("" //
				+ "substring {0} {10} {\"abcdefghij\"}"));
		assertEquals(eval("\"ef\""), eval("" //
				+ "substring {4} {6} {\"abcdefghij\"}"));
		assertEquals(eval("\"cdefgh\""), eval("" //
				+ "substring {2} {-2} {\"abcdefghij\"}"));
	}

	@Test
	public void testSwitch() {
		assertEquals(eval("\"C\""), eval("" //
				+ "define switch = (p => case \n" //
				+ "    || (p = 1) \"A\" \n" //
				+ "    || (p = 2) \"B\" \n" //
				+ "    || (p = 3) \"C\" \n" //
				+ "    || \"D\" \n" //
				+ ") >> \n" //
				+ "switch {3}"));
	}

	@Test
	public void testSys() {
		assertNotNull(Tree.decompose(eval("cons {1} {2,}")));
		assertEquals(Int.create(1), eval("head {1, 2, 3,}"));
		assertNotNull(Tree.decompose(eval("tail {1, 2, 3,}")));
	}

	@Test
	public void testTails() {
		assertEquals(SuiteUtil.parse("(1, 2, 3,), (2, 3,), (3,),"),
				eval("tails {1, 2, 3,}"));
	}

	@Test
	public void testTake() {
		assertEquals(SuiteUtil.parse("1, 2, 3, 4,"), eval("" //
				+ "take {4} {1, 2, 3, 4, 5, 6, 7,}"));
	}

	@Test
	public void testTranspose() {
		String r = "(1, 4, 7,), (2, 5, 8,), (3, 6, 9,),";
		assertEquals(SuiteUtil.parse(r), eval("" //
				+ "transpose {(1, 2, 3,), (4, 5, 6,), (7, 8, 9,),}"));
	}

	@Test
	public void testUniq() {
		assertEquals(SuiteUtil.parse("1, 2, 3, 5, 2,"), eval("" //
				+ "uniq {1, 2, 2, 2, 3, 5, 2,}"));
	}

	@Test
	public void testZip() {
		assertEquals(SuiteUtil.parse("(1, 5,), (2, 6,), (3, 7,),"), eval("" //
				+ "define zip-up = zip {a => b => a, b,} >> \n" //
				+ "zip-up {1, 2, 3,} {5, 6, 7,}"));
	}

	private static Node eval(String f) {
		return SuiteUtil.evaluateEagerFunctional(f);
	}

}
