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

package me.moros.bending.model.math;

import org.apache.commons.math3.util.FastMath;

public class LineSegment {
	public final Vector3 start;
	public final Vector3 end;

	public LineSegment(Vector3 start, Vector3 end) {
		this.start = start;
		this.end = end;
	}

	public Vector3 getClosestPoint(Vector3 target) {
		Vector3 toEnd = end.subtract(start);
		double t = target.subtract(start).dotProduct(toEnd) / toEnd.dotProduct(toEnd);
		// Cap t to the ends
		t = FastMath.max(0.0, FastMath.min(t, 1.0));
		return start.add(toEnd.scalarMultiply(t));
	}

	public Vector3 getDirection() {
		return end.subtract(start).normalize();
	}

	public Vector3 interpolate(double t) {
		t = FastMath.max(0.0, FastMath.min(t, 1.0));
		return start.add(end.subtract(start).scalarMultiply(t));
	}

	public double length() {
		return end.distance(start);
	}

	public double lengthSq() {
		return end.distanceSq(start);
	}
}
