package org.weiqi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.weiqi.Weiqi.Occupation;

public class EvaluatorTest {

	@Before
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testOneWhite() {
		Board board = new Board();
		board.set(Coordinate.c(9, 9), Occupation.WHITE);
		assertEquals(36110, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(-36110, Evaluator.evaluate(Occupation.BLACK, board));
	}

	@Test
	public void testEqualPower() {
		Board board = new Board();
		board.set(Coordinate.c(3, 3), Occupation.WHITE);
		board.set(Coordinate.c(15, 15), Occupation.BLACK);
		assertEquals(0, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(0, Evaluator.evaluate(Occupation.BLACK, board));
	}

	@Test
	public void testTerritory() {
		Board board = new Board();
		board.set(Coordinate.c(0, 3), Occupation.WHITE);
		board.set(Coordinate.c(1, 3), Occupation.WHITE);
		board.set(Coordinate.c(2, 3), Occupation.WHITE);
		board.set(Coordinate.c(3, 2), Occupation.WHITE);
		board.set(Coordinate.c(3, 1), Occupation.WHITE);
		board.set(Coordinate.c(3, 0), Occupation.WHITE);
		board.set(Coordinate.c(15, 15), Occupation.BLACK);
		assertEquals(1450, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(-1450, Evaluator.evaluate(Occupation.BLACK, board));
	}

}
