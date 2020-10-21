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
import org.checkerframework.checker.nullness.qual.NonNull;

public class AABB implements Collider {
	public static final AABB PLAYER_BOUNDS = new AABB(new Vector3(-0.3, 0.0, -0.3), new Vector3(0.3, 1.8, 0.3));
	public static final AABB BLOCK_BOUNDS = new AABB(Vector3.ZERO, Vector3.ONE);

	private final Vector3 min;
	private final Vector3 max;

	public AABB(@NonNull Vector3 min, @NonNull Vector3 max) {
		this.min = min;
		this.max = max;
	}

	public @NonNull AABB at(@NonNull Vector3 pos) {
		return new AABB(min.add(pos), max.add(pos));
	}

	public @NonNull AABB grow(@NonNull Vector3 diff) {
		return new AABB(min.subtract(diff), max.add(diff));
	}

	public @NonNull AABB scale(@NonNull Vector3 diff) {
		Vector3 extents = getHalfExtents();
		Vector3 newExtents = extents.multiply(diff);
		return grow(newExtents.subtract(extents));
	}

	public @NonNull Vector3 min() {
		return min;
	}

	public @NonNull Vector3 max() {
		return max;
	}

	public @NonNull Vector3 mid() {
		return min.add(max().subtract(min()).scalarMultiply(0.5));
	}

	public boolean contains(@NonNull Vector3 test) {
		return (test.getX() >= min.getX() && test.getX() <= max.getX()) &&
			(test.getY() >= min.getY() && test.getY() <= max.getY()) &&
			(test.getZ() >= min.getZ() && test.getZ() <= max.getZ());
	}

	public boolean intersects(@NonNull AABB other) {
		return (max.getX() > other.min.getX() &&
			min.getX() < other.max.getX() &&
			max.getY() > other.min.getY() &&
			min.getY() < other.max.getY() &&
			max.getZ() > other.min.getZ() &&
			min.getZ() < other.max.getZ());
	}

	public boolean intersects(@NonNull Sphere sphere) {
		return sphere.intersects(this);
	}

	public boolean intersects(@NonNull Ray ray) {
		Vector3 t0 = min.subtract(ray.origin).multiply(ray.invDir);
		Vector3 t1 = max.subtract(ray.origin).multiply(ray.invDir);
		Vector3 tmin = t0.min(t1);
		Vector3 tmax = t0.max(t1);
		return tmin.maxComponent() <= tmax.minComponent();
	}

	@Override
	public boolean intersects(@NonNull Collider collider) {
		if (collider instanceof Sphere) {
			return intersects((Sphere) collider);
		} else if (collider instanceof AABB) {
			return intersects((AABB) collider);
		} else if (collider instanceof OBB) {
			return collider.intersects(this);
		} else if (collider instanceof Disk) {
			return collider.intersects(this);
		}

		return false;
	}

	@Override
	public @NonNull Vector3 getPosition() {
		return mid();
	}

	@Override
	public @NonNull Vector3 getHalfExtents() {
		Vector3 half = max.subtract(min).scalarMultiply(0.5);
		return new Vector3(FastMath.abs(half.getX()), FastMath.abs(half.getY()), FastMath.abs(half.getZ()));
	}

	@Override
	public String toString() {
		return "[AABB min: " + min + ", max: " + max + "]";
	}
}
