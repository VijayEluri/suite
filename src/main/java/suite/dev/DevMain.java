package suite.dev;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.primitive.Chars_;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.Fail;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;

// mvn compile exec:java -Dexec.mainClass=suite.dev.DevMain -Dexec.args="${COLUMNS} ${LINES}"
public class DevMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	private int viewSizeX;
	private int viewSizeY;

	public static void main(String[] args) {
		int screenSizeX = Integer.valueOf(args[0]); // Integer.valueOf(System.getenv("COLUMNS"));
		int screenSizeY = Integer.valueOf(args[1]); // Integer.valueOf(System.getenv("LINES"));
		new DevMain(screenSizeX, screenSizeY).run();
	}

	private DevMain(int screenSizeX, int screenSizeY) {
		viewSizeX = screenSizeX;
		viewSizeY = screenSizeY - 1;
	}

	private void run() {
		List<String> input = Rethrow.ex(() -> Files.readAllLines(Paths.get("src/main/java/suite/dev/DevMain.java")));

		try (Termios termios = new Termios(libc);) {
			termios.clear();
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> state.apply((st, prev, text, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				String[] lines = Ints_ //
						.range(viewSizeY) //
						.map(screenY -> text.get(ox, oy + screenY, viewSizeX).replace('\t', ' ')) //
						.toArray(String.class);

				termios.cursor(false);

				for (int screenY = 0; screenY < viewSizeY; screenY++) {
					termios.gotoxy(0, screenY);
					termios.puts(lines[screenY]);
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = new State(null, text(input), c(0, 0), c(0, 0));
			redraw.sink(state0);

			FixieFun3<VK, Character, State, State> mutate = (vk, ch, state) -> state //
					.apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						if (vk == VK.LEFT_)
							return st.cursorCoord(c(cx - 1, cy));
						else if (vk == VK.RIGHT)
							return st.cursorCoord(c(cx + 1, cy));
						else if (vk == VK.UP___)
							return st.cursorCoord(c(cx, cy - 1));
						else if (vk == VK.DOWN_)
							return st.cursorCoord(c(cx, cy + 1));
						else if (vk == VK.PGUP_)
							return st.cursorCoord(c(cx, cy - viewSizeY));
						else if (vk == VK.PGDN_)
							return st.cursorCoord(c(cx, cy + viewSizeY));
						else if (vk == VK.HOME_)
							return st.cursorCoord(c(0, cy));
						else if (vk == VK.END__)
							return st.cursorCoord(text.coord(text.ends[cy]));
						else if (vk == VK.CTRL_LEFT_) {
							int index = text.index(cx, cy), index1;
							while (0 <= (index1 = index - 1) && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursorCoord(text.coord(index));
						} else if (vk == VK.CTRL_RIGHT) {
							int index = text.index(cx, cy), index1;
							while ((index1 = index + 1) < text.length() && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursorCoord(text.coord(index));
						} else if (vk == VK.CTRL_UP___) {
							int oy1 = max(cy - viewSizeY + 1, 0);
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.text(text).offsetCoord(c(ox, oy - viewSizeY)).cursorCoord(c(cx, cy - viewSizeY));
						} else if (vk == VK.CTRL_DOWN_) {
							int oy1 = min(cy, text.nLines());
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.text(text).offsetCoord(c(ox, oy + viewSizeY)).cursorCoord(c(cx, cy + viewSizeY));
						} else if (vk == VK.ALT_J____) {
							int index = text.index(cx, cy);
							while (index < text.length() && text.at(index) != '\n')
								index++;
							Text text1 = text.splice(index, 1, "");
							return st.text(text1).cursorCoord(text1.coord(index));
						} else if (vk == VK.BKSP_) {
							int index = text.index(cx, cy);
							if (0 < index) {
								IntIntPair cc1 = text.coord(index - 1);
								return st.text(text.splice(cc1, 1, "")).cursorCoord(cc1);
							} else
								return st;
						} else if (vk == VK.DEL__)
							return st.text(text.splice(cc, 1, ""));
						else if (vk == VK.CTRL_C____)
							return Fail.t();
						else if (vk == VK.CTRL_Z____) {
							State parent0 = st.previous;
							State parent1 = parent0 != null ? parent0 : st;
							return new State(parent1.previous, parent1.text, oc, parent1.cursorCoord);
						} else if (ch != null)
							if (ch == 13)
								return st.text(text.splice(cc, 0, "\n")).cursorCoord(c(0, cy + 1));
							else
								return st.text(text.splice(cc, 0, Character.toString(ch))).cursorCoord(c(cx + 1, cy));
						else
							return st;
					}))).apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int nLines = text.nLines();
						int cy_ = sat(cy, nLines);
						int cx_ = cy_ < nLines ? sat(cx, text.lineLength(cy_)) : 0;
						return st.cursorCoord(c(cx_, cy_));
					}))).apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ox_ = sat(ox, cx - viewSizeX + 1, cx);
						int oy_ = sat(oy, cy - viewSizeY + 1, cy);
						return st.offsetCoord(c(ox_, oy_));
					})));

			keyboard.loop(signal -> signal //
					.fold(state0, (state, pair_) -> pair_.map((vk, ch) -> mutate.apply(vk, ch, state))) //
					.wire(redraw));
		}
	}

	private class State {
		private State previous;
		private Text text;
		private IntIntPair offsetCoord;
		private IntIntPair cursorCoord;

		private State(State previous, Text text, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			this.previous = previous;
			this.text = text;
			this.offsetCoord = offsetCoord;
			this.cursorCoord = cursorCoord;
		}

		private State text(Text text) {
			State state = this;
			for (int i = 0; i < 16; i++)
				if (state != null)
					state = state.previous;
				else
					break;
			if (state != null)
				state.previous = null;
			return new State(this, text, offsetCoord, cursorCoord);
		}

		private State offsetCoord(IntIntPair offsetCoord) {
			return new State(previous, text, offsetCoord, cursorCoord);
		}

		private State cursorCoord(IntIntPair cursorCoord) {
			return new State(previous, text, offsetCoord, cursorCoord);
		}

		private <R> R apply(FixieFun5<State, State, Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(this, previous, text, offsetCoord, cursorCoord);
		}
	}

	private Text text(String text) {
		return text(Arrays.asList(text.split("\n")));
	}

	private Text text(List<String> lines) {
		int size = lines.size();
		int[] starts = new int[size];
		int[] ends = new int[size];
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < size; i++) {
			starts[i] = sb.length();
			sb.append(lines.get(i));
			ends[i] = sb.length();
			sb.append("\n");
		}

		return new Text(sb.toString(), starts, ends);
	}

	private class Text {
		private String text;
		private int[] starts;
		private int[] ends;

		private Text(String text, int[] starts, int[] ends) {
			this.text = text;
			this.starts = starts;
			this.ends = ends;
		}

		private String get(int px, int py, int length) {
			int i0 = starts[py] + px;
			int ix = ends[py];
			return new String(Chars_.toArray(length, i_ -> {
				int i = i_ + i0;
				return i < ix ? text.charAt(i) : ' ';
			}));
		}

		private Text splice(IntIntPair p, int deletes, String s) {
			return splice(index(p.t0, p.t1), deletes, s);
		}

		private Text splice(int index, int deletes, String s) {
			int length = text.length();
			int i1 = min(index + deletes, length);
			return text(text.substring(0, index) + s + text.substring(i1, length));
		}

		private int index(int px, int py) {
			if (py < nLines())
				return min(starts[py] + px, ends[py]);
			else
				return text.length();
		}

		private IntIntPair coord(int index) {
			int nLines = nLines();
			int y = 0, y1;
			while ((y1 = y + 1) < nLines && starts[y1] < index)
				y = y1;
			return c(index - starts[y], y);
		}

		private int nLines() {
			return starts.length;
		}

		private int lineLength(int y) {
			return ends[y] - starts[y];
		}

		private char at(int arg0) {
			return text.charAt(arg0);
		}

		private int length() {
			return text.length();
		}
	}

	private static IntIntPair c(int x, int y) {
		return IntIntPair.of(x, y);
	}

	private static int sat(int x, int max) {
		return sat(x, 0, max);
	}

	private static int sat(int x, int min, int max) {
		return min(max(x, min), max);
	}

}
