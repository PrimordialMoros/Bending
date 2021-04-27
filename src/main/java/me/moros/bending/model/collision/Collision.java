/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

package me.moros.bending.model.collision;

import java.util.Map.Entry;

import com.google.common.collect.Maps;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.Ability;

/**
 * Represents a real collision between 2 abilities.
 */
public final class Collision {
	private final CollisionData collisionData;
	private final boolean inverse;

	private Collision(CollisionData collisionData, boolean inverse) {
		this.collisionData = collisionData;
		this.inverse = inverse;
	}

	public @NonNull Ability getCollidedAbility() {
		return inverse ? collisionData.first : collisionData.second;
	}

	public @NonNull Entry<Collider, Collider> getColliders() {
		if (inverse) {
			return Maps.immutableEntry(collisionData.c2, collisionData.c1);
		} else {
			return Maps.immutableEntry(collisionData.c1, collisionData.c2);
		}
	}

	public boolean shouldRemoveSelf() {
		return inverse ? collisionData.removeSecond : collisionData.removeFirst;
	}

	public void setRemoveSelf(boolean value) {
		if (inverse) {
			collisionData.removeSecond = value;
		} else {
			collisionData.removeFirst = value;
		}
	}

	public boolean shouldRemoveCollided() {
		return inverse ? collisionData.removeFirst : collisionData.removeSecond;
	}

	public void setRemoveCollided(boolean value) {
		if (inverse) {
			collisionData.removeFirst = value;
		} else {
			collisionData.removeSecond = value;
		}
	}

	public static class CollisionData {
		private final Ability first, second;
		private final Collider c1, c2;
		private boolean removeFirst, removeSecond;


		public CollisionData(@NonNull Ability first, @NonNull Ability second, @NonNull Collider c1, @NonNull Collider c2, boolean removeFirst, boolean removeSecond) {
			this.first = first;
			this.second = second;
			this.c1 = c1;
			this.c2 = c2;
			this.removeFirst = removeFirst;
			this.removeSecond = removeSecond;
		}

		public boolean shouldRemoveFirst() {
			return removeFirst;
		}

		public boolean shouldRemoveSecond() {
			return removeSecond;
		}

		public @NonNull Collision asCollision() {
			return new Collision(this, false);
		}

		public @NonNull Collision asInverseCollision() {
			return new Collision(this, true);
		}
	}
}
