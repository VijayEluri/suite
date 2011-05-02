package org.weiqi;

import java.util.Iterator;

public class Coordinate {

	private int x, y;

	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int hashCode() {
		return x << 8 + y;
	}

	public int getArrayPosition() {
		return x * Weiqi.SIZE + y;
	}

	public boolean equals(Object object) {
		if (object instanceof Coordinate) {
			Coordinate c = (Coordinate) object;
			return x == c.x && y == c.y;
		} else
			return false;
	}

	public boolean isWithinBoard() {
		return 0 <= x && x < Weiqi.SIZE && 0 <= y && y < Weiqi.SIZE;
	}

	public Coordinate[] leftOrUp() {
		Coordinate left = x > 0 ? new Coordinate(x - 1, y) : null;
		Coordinate up = y > 0 ? new Coordinate(x, y - 1) : null;

		if (left == null || up == null)
			if (left != null)
				return new Coordinate[] { left };
			else if (up != null)
				return new Coordinate[] { up };
			else
				return new Coordinate[] {};
		else
			return new Coordinate[] { left, up };
	}

	public Iterable<Coordinate> getNeighbours() {
		return new Iterable<Coordinate>() {
			public Iterator<Coordinate> iterator() {
				return new Iterator<Coordinate>() {
					public int n = 0;

					public boolean hasNext() {
						return n < 4;
					}

					public Coordinate next() {
						switch (n++) {
						case 0:
							return new Coordinate(x + 1, y);
						case 1:
							return new Coordinate(x - 1, y);
						case 2:
							return new Coordinate(x, y + 1);
						case 3:
							return new Coordinate(x, y - 1);
						default:
							throw new RuntimeException("Run out of neighbours");
						}
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static Iterable<Coordinate> getAll() {
		return new Iterable<Coordinate>() {
			public Iterator<Coordinate> iterator() {
				return new Iterator<Coordinate>() {
					public Coordinate c = new Coordinate(0, 0);

					public boolean hasNext() {
						return c.x < Weiqi.SIZE && c.y < Weiqi.SIZE;
					}

					public Coordinate next() {
						Coordinate ret = new Coordinate(c.x, c.y);
						c.y++;
						if (c.y == Weiqi.SIZE) {
							c.x++;
							c.y = 0;
						}
						return ret;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

}
