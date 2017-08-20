package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class FunInterpreterLazyTest {

	@Test
	public void testDecons() {
		expect("let l := (1; 3;) >> if (l = `$a; $b;`) then b else error ()", Int.of(3));
	}

	@Test
	public void testFibonacci() {
		expect("define fib := (n => if (1 < n) then (fib {n - 1} + fib {n - 2}) else n) >> fib {12}", Int.of(144));
	}

	@Test
	public void testLets() {
		expect("lets (a := b + 2 # b := 1 #) >> a", Int.of(3));
		expect("lets (b := 1 # a := b + 2 #) >> a", Int.of(3));
	}

	@Test
	public void testNestedFunction() {
		expect("define inc := (define inc_ := (x => x + 1) >> inc_) >> inc {3}", Suite.parse("4"));
	}

	private void expect(String expr, Node expected) {
		expect(true, expr, expected);
		expect(false, expr, expected);
	}

	private void expect(boolean isLazyify, String expr, Node expected) {
		FunInterpreterEager interpreter = new FunInterpreterEager();
		interpreter.setLazyify(isLazyify);
		assertEquals(expected, interpreter.eager(Suite.parse(expr)));
	}
}
