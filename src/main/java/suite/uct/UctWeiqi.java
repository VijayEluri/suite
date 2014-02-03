package suite.uct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import suite.util.Util;
import suite.weiqi.Board;
import suite.weiqi.Board.MoveType;
import suite.weiqi.Coordinate;
import suite.weiqi.Evaluator;
import suite.weiqi.GameSet;
import suite.weiqi.GameSet.Move;
import suite.weiqi.Weiqi;
import suite.weiqi.Weiqi.Occupation;

public class UctWeiqi {

	public static class Visitor implements UctVisitor<Coordinate> {
		private final GameSet gameSet;
		private final Board board;

		private Visitor(GameSet gameSet) {
			this.gameSet = gameSet;
			board = gameSet.getBoard();
		}

		@Override
		public Iterable<Coordinate> getAllMoves() {
			return Coordinate.all();
		}

		@Override
		public List<Coordinate> elaborateMoves() {
			Move move = new Move();
			List<Coordinate> captureMoves = new ArrayList<>();
			List<Coordinate> otherMoves = new ArrayList<>();

			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY) {
					move.position = c;

					if (gameSet.isValidMove(move))
						if (move.type == MoveType.CAPTURE)
							ShuffleUtil.add(captureMoves, c);
						else
							ShuffleUtil.add(otherMoves, c);
				}

			// Make capture moves at the head;
			// UctSearch would put them in first few nodes
			captureMoves.addAll(otherMoves);
			return captureMoves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.play(c);
		}

		/**
		 * The "play till both passes" Monte Carlo, with some customizations:
		 * 
		 * - Consider capture moves first;
		 * 
		 * - Would not fill a single-eye.
		 */
		@Override
		public boolean evaluateRandomOutcome() {
			List<Coordinate> empties = findAllEmptyPositions();
			Set<Coordinate> capturedPositions = new HashSet<>();
			Occupation me = gameSet.getNextPlayer();
			Move move, chosenMove;
			int nPasses = 0;

			// Move until someone cannot move anymore, or maximum number of
			// passes is reached between both players
			while (nPasses < 2) {
				Iterator<Coordinate> iter = empties.iterator();
				chosenMove = null;

				while (chosenMove == null && iter.hasNext()) {
					Coordinate c = iter.next();
					boolean isFillEye = true;

					for (Coordinate c1 : c.neighbors())
						isFillEye &= board.get(c1) == gameSet.getNextPlayer();

					if (!isFillEye && gameSet.playIfValid(move = new Move(c))) {
						iter.remove();
						chosenMove = move;
					}
				}

				if (chosenMove != null) {
					if (chosenMove.type == MoveType.CAPTURE) {
						int i = 0;
						capturedPositions.clear();

						// Add captured positions back to empty group
						for (Coordinate c1 : chosenMove.position.neighbors()) {
							Occupation neighborColor = chosenMove.neighborColors[i++];
							if (neighborColor != board.get(c1))
								capturedPositions.addAll(board.findGroup(c1));
						}

						for (Coordinate c2 : capturedPositions)
							ShuffleUtil.add(empties, c2);
					}

					nPasses = 0;
				} else {
					gameSet.pass();
					nPasses++;
				}
			}

			return Evaluator.evaluate(me, board) > 0;
		}

		/**
		 * The "play till any player cannot move" version of Monte Carlo.
		 */
		public boolean evaluateRandomOutcome0() {
			Occupation me = gameSet.getNextPlayer();
			List<Coordinate> empties = findAllEmptyPositions();
			Coordinate pos;
			Move move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 4 * Weiqi.area; i++) {
				move = null;

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				if ((pos = Util.last(empties)) != null)
					if (gameSet.playIfValid(move = new Move(pos)))
						empties.remove(empties.size() - 1);
					else
						move = null;

				if (move == null)
					move = removePossibleMove(empties.iterator());

				if (move != null) { // Add empty positions back to empty group
					int j = 0;

					for (Coordinate c1 : move.position.neighbors())
						if (move.neighborColors[j++] != board.get(c1))
							for (Coordinate c2 : board.findGroup(c1))
								ShuffleUtil.add(empties, c2);
				} else
					break; // No moves can be played, current player lost
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return Evaluator.evaluate(me, board) > 0;
		}

		private Move removePossibleMove(Iterator<Coordinate> iter) {
			while (iter.hasNext()) {
				Move move = new Move(iter.next());

				if (gameSet.playIfValid(move)) {
					iter.remove();
					return move;
				}
			}

			return null;
		}

		private List<Coordinate> findAllEmptyPositions() {
			List<Coordinate> moves = new ArrayList<>();

			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY)
					ShuffleUtil.add(moves, c);

			return moves;
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new GameSet(gameSet));
		}
	}

	public static Visitor createVisitor(GameSet gameSet) {
		return new Visitor(gameSet);
	}

}
