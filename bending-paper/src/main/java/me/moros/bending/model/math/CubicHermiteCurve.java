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

// Represents a curve between a start and end point.
public class CubicHermiteCurve {
	private static final int LENGTH_INTERPOLATION_COUNT = 5;

	private final Vector3 startPoint;
	private final Vector3 startTangent;
	private final Vector3 endPoint;
	private final Vector3 endTangent;
	private final double chordalDistance;

	public CubicHermiteCurve(Vector3 startPoint, Vector3 startTanget, Vector3 endPoint, Vector3 endTangent) {
		this.startPoint = startPoint;
		this.startTangent = startTanget;
		this.endPoint = endPoint;
		this.endTangent = endTangent;
		// Approximate arc-length by just using chordal distance.
		this.chordalDistance = endPoint.distance(startPoint);
	}

	public Vector3 interpolate(double t) {
		t = FastMath.max(0.0, FastMath.min(t, 1.0));
		double startPointT = (2.0 * t * t * t) - (3.0 * t * t) + 1.0;
		double startTangentT = (t * t * t) - (2.0 * t * t) + t;
		double endPointT = (-2.0 * t * t * t) + (3.0 * t * t);
		double endTangentT = (t * t * t) - (t * t);
		return startPoint.scalarMultiply(startPointT)
			.add(startTangent.scalarMultiply(startTangentT))
			.add(endPoint.scalarMultiply(endPointT))
			.add(endTangent.scalarMultiply(endTangentT));
	}

	// Approximate length by summing length of n interpolated lines.
	public double getLength() {
		double result = 0.0;
		Vector3 current = startPoint;
		for (int i = 0; i < LENGTH_INTERPOLATION_COUNT; ++i) {
			Vector3 interpolated = interpolate(i / (double) LENGTH_INTERPOLATION_COUNT);
			result += current.distance(interpolated);
			current = interpolated;
		}
		return result;
	}
}
