/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.common;

public abstract class Grid {
	private final int size;
	protected final int[][] grid;

	public Grid(int size) {
		this.size = size;
		grid = new int[size + 1][size + 1];
	}

	public abstract void draw();

	public void update() {
		reset();
		draw();
	}

	public int getValue(int x, int y) {
		int centerPoint = size / 2;
		return grid[x + centerPoint][y + centerPoint];
	}

	public void setLocation(int x, int y, int set) {
		int centerPoint = size / 2;
		grid[x + centerPoint][y + centerPoint] = set;
	}

	public void xLine(int x1, int x2, int y, int value) {
		while (x1 <= x2) {
			setLocation(x1++, y, value);
		}
	}

	public void yLine(int x, int y1, int y2, int value) {
		while (y1 <= y2) {
			setLocation(x, y1++, value);
		}
	}

	public void drawCircle(int x0, int y0, int radius, int value) {
		int x = radius;
		int y = 0;
		int err = 0;

		while (x >= y) {
			setLocation(x0 + x, y0 + y, value);
			setLocation(x0 + y, y0 + x, value);
			setLocation(x0 - y, y0 + x, value);
			setLocation(x0 - x, y0 + y, value);
			setLocation(x0 - x, y0 - y, value);
			setLocation(x0 - y, y0 - x, value);
			setLocation(x0 + y, y0 - x, value);
			setLocation(x0 + x, y0 - y, value);

			y += 1;
			err += 1 + 2 * y;
			if (2 * (err - x) + 1 > 0) {
				x -= 1;
				err += 1 - 2 * x;
			}
		}
	}

	public void drawCircle(int xc, int yc, int inner, int outer, int value) {
		int xo = outer;
		int xi = inner;
		int y = 0;
		int erro = 1 - xo;
		int erri = 1 - xi;

		while (xo >= y) {
			xLine(xc + xi, xc + xo, yc + y, value);
			yLine(xc + y, yc + xi, yc + xo, value);
			xLine(xc - xo, xc - xi, yc + y, value);
			yLine(xc - y, yc + xi, yc + xo, value);
			xLine(xc - xo, xc - xi, yc - y, value);
			yLine(xc - y, yc - xo, yc - xi, value);
			xLine(xc + xi, xc + xo, yc - y, value);
			yLine(xc + y, yc - xo, yc - xi, value);

			++y;

			if (erro < 0) {
				erro += 2 * y + 1;
			} else {
				--xo;
				erro += 2 * (y - xo + 1);
			}

			if (y > inner) {
				xi = y;
			} else {
				if (erri < 0) {
					erri += 2 * y + 1;
				} else {
					xi--;
					erri += 2 * (y - xi + 1);
				}
			}
		}
	}

	public int[][] getGrid() {
		return grid;
	}

	public void reset() {
		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				grid[x][y] = 0;
			}
		}
	}
}
