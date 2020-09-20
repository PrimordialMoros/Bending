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

public final class DummyCollider extends AABB {
	public DummyCollider() {
		super(Vector3.ZERO, Vector3.ZERO);
	}

	@Override
	public AABB at(Vector3 pos) {
		return this;
	}

	@Override
	public AABB grow(double x, double y, double z) {
		return this;
	}

	@Override
	public AABB scale(double x, double y, double z) {
		return this;
	}

	@Override
	public AABB scale(double amount) {
		return this;
	}

	@Override
	public Vector3 min() {
		return Vector3.ZERO;
	}

	@Override
	public Vector3 max() {
		return Vector3.ZERO;
	}

	@Override
	public Vector3 mid() {
		return Vector3.ZERO;
	}

	@Override
	public boolean contains(Vector3 test) {
		return false;
	}

	@Override
	public boolean intersects(AABB other) {
		return false;
	}

	@Override
	public boolean intersects(Sphere sphere) {
		return false;
	}

	@Override
	public boolean intersects(Ray ray) {
		return false;
	}

	@Override
	public boolean intersects(Collider collider) {
		return false;
	}

	@Override
	public Vector3 getPosition() {
		return Vector3.ZERO;
	}

	@Override
	public Vector3 getHalfExtents() {
		return Vector3.ZERO;
	}

	@Override
	public String toString() {
		return "DummyCollider";
	}
}
