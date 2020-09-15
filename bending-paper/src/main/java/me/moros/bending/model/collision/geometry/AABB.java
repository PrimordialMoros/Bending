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

package me.moros.bending.model.collision.geometry;

import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3;
import org.apache.commons.math3.util.FastMath;

public class AABB implements Collider {
	public static final AABB PLAYER_BOUNDS = new AABB(new Vector3(-0.3, 0.0, -0.3), new Vector3(0.3, 1.8, 0.3));
	public static final AABB BLOCK_BOUNDS = new AABB(new Vector3(0.0, 0.0, 0.0), new Vector3(1.0, 1.0, 1.0));

	private final Vector3 min;
	private final Vector3 max;

	public AABB(Vector3 min, Vector3 max) {
		this.min = min;
		this.max = max;
	}

	public AABB at(Vector3 pos) {
		return new AABB(min.add(pos), max.add(pos));
	}

	public AABB grow(double x, double y, double z) {
		Vector3 change = new Vector3(x, y, z);
		return new AABB(min.subtract(change), max.add(change));
	}

	public AABB scale(double x, double y, double z) {
		Vector3 extents = getHalfExtents();
		Vector3 newExtents = extents.multiply(x, y, z);
		Vector3 diff = newExtents.subtract(extents);
		return grow(diff.getX(), diff.getY(), diff.getZ());
	}

	public AABB scale(double amount) {
		Vector3 extents = getHalfExtents();
		Vector3 newExtents = extents.scalarMultiply(amount);
		Vector3 diff = newExtents.subtract(extents);

		return grow(diff.getX(), diff.getY(), diff.getZ());
	}

	public Vector3 min() {
		return this.min;
	}

	public Vector3 max() {
		return this.max;
	}

	public Vector3 mid() {
		return this.min.add(this.max().subtract(this.min()).scalarMultiply(0.5));
	}

	public boolean contains(Vector3 test) {
		return (test.getX() >= min.getX() && test.getX() <= max.getX()) &&
			(test.getY() >= min.getY() && test.getY() <= max.getY()) &&
			(test.getZ() >= min.getZ() && test.getZ() <= max.getZ());
	}

	public boolean intersects(AABB other) {
		return (max.getX() > other.min.getX() &&
			min.getX() < other.max.getX() &&
			max.getY() > other.min.getY() &&
			min.getY() < other.max.getY() &&
			max.getZ() > other.min.getZ() &&
			min.getZ() < other.max.getZ());
	}

	public boolean intersects(Sphere sphere) {
		return sphere.intersects(this);
	}

	public boolean intersects(Ray ray) {
		Vector3 t0 = min.subtract(ray.origin).multiply(ray.invDir);
		Vector3 t1 = max.subtract(ray.origin).multiply(ray.invDir);
		Vector3 tmin = t0.min(t1);
		Vector3 tmax = t0.max(t1);
		return tmin.maxComponent() <= tmax.minComponent();
	}

	@Override
	public boolean intersects(Collider collider) {
		if (collider instanceof Sphere) {
			return intersects((Sphere) collider);
		} else if (collider instanceof AABB) {
			return intersects((AABB) collider);
		} else if (collider instanceof OBB) {
			return collider.intersects(this);
		} else if (collider instanceof Disc) {
			return collider.intersects(this);
		}

		return false;
	}

	@Override
	public Vector3 getPosition() {
		return mid();
	}

	@Override
	public Vector3 getHalfExtents() {
		Vector3 half = max.subtract(min).scalarMultiply(0.5);
		return new Vector3(Math.abs(half.getX()), FastMath.abs(half.getY()), FastMath.abs(half.getZ()));
	}

	@Override
	public String toString() {
		return "[AABB min: " + min + ", max: " + max + "]";
	}
}
