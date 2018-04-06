package suite.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import suite.util.To;
import suite.weiqi.Weiqi.Occupation;

public class BoardTest {

	@Before
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testNeighbor() {
		var c = Coordinate.c(10, 10);
		var neighbors = new HashSet<>();

		for (var c1 : c.neighbors)
			neighbors.add(c1);

		assertEquals(To.set( //
				Coordinate.c(9, 10) //
				, Coordinate.c(11, 10) //
				, Coordinate.c(10, 9) //
				, Coordinate.c(10, 11) //
		), neighbors);
	}

	@Test
	public void testEat() {
		var board = new Board();
		board.playIfSeemsPossible(Coordinate.c(0, 1), Occupation.BLACK);
		board.playIfSeemsPossible(Coordinate.c(0, 0), Occupation.WHITE);
		board.playIfSeemsPossible(Coordinate.c(1, 0), Occupation.BLACK);
		assertEquals(board.get(Coordinate.c(0, 0)), Occupation.EMPTY);
	}

	@Test
	public void testGroupAnalysis() {
		var board = blackBoard();
		board.set(Coordinate.c(3, 3), Occupation.EMPTY);
		board.set(Coordinate.c(3, 4), Occupation.EMPTY);
		board.set(Coordinate.c(7, 3), Occupation.EMPTY);
		board.set(Coordinate.c(18, 0), Occupation.EMPTY);
		board.set(Coordinate.c(17, 1), Occupation.EMPTY);
		board.set(Coordinate.c(17, 0), Occupation.WHITE);
		board.set(Coordinate.c(18, 1), Occupation.WHITE);

		var ga = new GroupAnalysis(board);
		var blackGroup = ga.getGroup(Coordinate.c(15, 15));
		var whiteGroup = ga.getGroup(Coordinate.c(17, 0));

		assertEquals(361 - 7, blackGroup.coords.size());
		assertEquals(3, whiteGroup.touches.size());
		assertTrue(blackGroup.touches.contains(whiteGroup));
		assertTrue(whiteGroup.touches.contains(blackGroup));
		assertEquals(4, blackGroup.breathes.size());
	}

	private Board blackBoard() {
		var board = new Board();
		for (var c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
