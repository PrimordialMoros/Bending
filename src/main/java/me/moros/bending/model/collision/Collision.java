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

import me.moros.bending.model.ability.Ability;

/**
 * Represents a real collision between 2 abilities.
 */
public final class Collision {
	private final Ability firstAbility;
	private final Ability secondAbility;
	private final boolean removeFirst;
	private final boolean removeSecond;
	private final Collider firstCollider;
	private final Collider secondCollider;

	public Collision(Ability first, Ability second, boolean removeFirst, boolean removeSecond, Collider firstCollider, Collider secondCollider) {
		this.firstAbility = first;
		this.secondAbility = second;
		this.removeFirst = removeFirst;
		this.removeSecond = removeSecond;
		this.firstCollider = firstCollider;
		this.secondCollider = secondCollider;
	}

	public Ability getFirstAbility() {
		return firstAbility;
	}

	public Ability getSecondAbility() {
		return secondAbility;
	}

	public boolean shouldRemoveFirst() {
		return removeFirst;
	}

	public boolean shouldRemoveSecond() {
		return removeSecond;
	}

	public Collider getFirstCollider() {
		return firstCollider;
	}

	public Collider getSecondCollider() {
		return secondCollider;
	}
}
