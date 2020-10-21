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

import java.util.ArrayList;
import java.util.List;

// Generates a spline, using cubic hermite curves, from a set of knots.
// Set tension to 0 to generate a Catmull-Rom spline.
public class CubicHermiteSpline {
	private final List<Vector3> knots = new ArrayList<>();
	private final List<CubicHermiteCurve> curves = new ArrayList<>();
	private double length;
	private boolean built;
	private final double tension;

	public CubicHermiteSpline() {
		this(0.0);
	}

	public CubicHermiteSpline(double tension) {
		this.tension = tension;
	}

	public void addKnot(Vector3 position) {
		knots.add(position);
		built = false;
	}

	public List<Vector3> getKnots() {
		return knots;
	}

	public Vector3 interpolate(double t) {
		t = FastMath.max(0.0, FastMath.min(t, 1.0));
		if (!built) {
			build();
		}
		double tStart = 0.0;
		for (CubicHermiteCurve curve : curves) {
			double tEnd = tStart + FastMath.max(curve.getLength(), 0.000001) / length;
			if (tStart <= t && t <= tEnd) {
				double curveT = (t - tStart) / (tEnd - tStart);
				return curve.interpolate(curveT);
			}
			tStart += (tEnd - tStart);
		}
		return curves.get(curves.size() - 1).interpolate(1.0);
	}

	public void build() {
		Vector3 startPoint, startTangent, endPoint, endTangent;
		length = 0.0;
		curves.clear();

		for (int i = 0; i < knots.size() - 1; ++i) {
			startPoint = knots.get(i);
			endPoint = knots.get(i + 1);
			// Compute tangents
			if (i > 0) {
				startTangent = endPoint.subtract(knots.get(i - 1)).scalarMultiply(0.5);
			} else {
				startTangent = endPoint.subtract(startPoint);
			}
			if (i < knots.size() - 2) {
				endTangent = knots.get(i + 2).subtract(startPoint).scalarMultiply(0.5);
			} else {
				endTangent = endPoint.subtract(startPoint);
			}
			// Apply tension to the tangents
			startTangent = startTangent.scalarMultiply(1.0 - tension);
			endTangent = endTangent.scalarMultiply(1.0 - tension);
			CubicHermiteCurve curve = new CubicHermiteCurve(startPoint, startTangent, endPoint, endTangent);
			length += curve.getLength();
			curves.add(curve);
		}
		built = true;
	}

	public double getLength() {
		return length;
	}
}
