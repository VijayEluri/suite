package suite.weiqi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctVisitor;
import suite.uct.UctWeiqi;
import suite.util.FileUtil;
import suite.util.Pair;
import suite.util.Util;
import suite.weiqi.Weiqi.Occupation;

/**
 * How I did profiling:
 * 
 * java -agentlib:hprof=cpu=times,depth=16,interval=1,thread=y
 */
public class UctWeiqiMain<Move> {

	private static final Occupation computerPlayer = Occupation.BLACK;
	private static final Occupation humanPlayer = Occupation.WHITE;

	private static final Occupation startingPlayer = Occupation.BLACK;

	public static void main(String args[]) throws IOException {
		InputStreamReader isr = new InputStreamReader(System.in, FileUtil.charset);
		BufferedReader br = new BufferedReader(isr);
		DecimalFormat df = new DecimalFormat("0.000");
		int nThreads = Runtime.getRuntime().availableProcessors();
		int nSimulations = 10000 * nThreads;
		int boundedTime = 30000;
		Weiqi.adjustSize(7);

		Board board = new Board();
		MovingGameSet gameSet = new MovingGameSet(board, startingPlayer);
		boolean auto = false;
		boolean quit = false;
		String status = "LET'S PLAY!";

		while (!quit) {
			GameSet gameSet1 = new GameSet(gameSet);
			UctVisitor<Coordinate> visitor = UctWeiqi.createVisitor(gameSet1);
			UctSearch<Coordinate> search = new UctSearch<>(visitor);
			search.setNumberOfThreads(nThreads);
			search.setNumberOfSimulations(nSimulations);
			search.setBoundedTime(boundedTime);

			if (auto || gameSet.getNextPlayer() == computerPlayer) {
				System.out.println("THINKING...");

				long start = System.currentTimeMillis();
				Coordinate coord = search.search();
				long end = System.currentTimeMillis();

				if (coord != null) {
					status = gameSet.getNextPlayer() //
							+ " " + coord //
							+ " " + df.format(search.getWinningChance()) //
							+ " " + (end - start) + "ms";

					gameSet.play(coord);

					if (auto)
						display(gameSet, status);
				} else {
					System.out.println("I LOSE");
					quit = true;
				}
			}

			while (!auto && !quit && gameSet.getNextPlayer() == humanPlayer)
				try {
					display(gameSet, status);

					String line = br.readLine();

					if (line != null)
						switch (line) {
						case "auto":
							auto = true;
							break;
						case "load":
							gameSet = loadGameSet(br);
							break;
						case "undo":
							gameSet.undo();
							gameSet.undo();
							status = "AFTER UNDO:";
							break;
						default:
							if (!Util.isBlank(line)) {
								Pair<String, String> pos = Util.split2(line, ",");
								int x = Integer.parseInt(pos.t0);
								int y = Integer.parseInt(pos.t1);
								gameSet.play(Coordinate.c(x, y));
							}
						}
					else
						quit = true;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
		}
	}

	private static void display(MovingGameSet gameSet, String status) {
		System.out.println(status);
		UserInterface.display(gameSet);
	}

	private static MovingGameSet loadGameSet(BufferedReader br) throws IOException {
		System.out.println("PLEASE ENTER BOARD DATA AND AN BLANK LINE:\n");
		String s;
		StringBuilder sb = new StringBuilder();

		do
			sb.append((s = br.readLine()) + "\n");
		while (!Util.isBlank(s));

		Board board = UserInterface.importBoard(sb.toString());
		return new MovingGameSet(board, startingPlayer);
	}

	protected static void deepThink() {
		int seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);

		GameSet gameSet = new GameSet(new Board(), startingPlayer);

		UctVisitor<Coordinate> visitor = UctWeiqi.createVisitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		Coordinate move = search.search();
		gameSet.play(move);

		// search.dumpSearch();
		System.out.println(move);
	}

}
