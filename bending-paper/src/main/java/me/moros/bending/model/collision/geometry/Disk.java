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
import org.checkerframework.checker.nullness.qual.NonNull;

// Combines an OBB and Sphere to create a disc-like collider.
public class Disk implements Collider {
	private final Sphere sphere;
	private final OBB obb;

	public Disk(@NonNull OBB obb, @NonNull Sphere sphere) {
		this.obb = obb;
		this.sphere = sphere;
	}

	public @NonNull Disk addPosition(@NonNull Vector3 position) {
		return new Disk(this.obb.addPosition(position), this.sphere.at(position));
	}

	public @NonNull Disk at(@NonNull Vector3 position) {
		return new Disk(this.obb.at(position), this.sphere.at(position));
	}

	@Override
	public boolean intersects(@NonNull Collider collider) {
		return sphere.intersects(collider) && obb.intersects(collider);
	}

	@Override
	public @NonNull Vector3 getPosition() {
		return sphere.center;
	}

	@Override
	public @NonNull Vector3 getHalfExtents() {
		return obb.getHalfExtents();
	}

	@Override
	public boolean contains(@NonNull Vector3 point) {
		return sphere.contains(point) && obb.contains(point);
	}

	public @NonNull OBB getOBB() {
		return obb;
	}

	public @NonNull Sphere getSphere() {
		return sphere;
	}
}
