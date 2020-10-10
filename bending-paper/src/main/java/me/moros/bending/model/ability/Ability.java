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

package me.moros.bending.model.ability;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;

import java.util.Collection;
import java.util.Collections;

public interface Ability extends Updatable {
	boolean activate(User user, ActivationMethod method); // return true if the ability was activated

	void onDestroy();

	User getUser();

	String getName();

	default boolean setUser(User newUser) {
		return false;
	}

	default boolean isStatic() {
		return false;
	}

	default AbilityDescription getDescription() {
		return Game.getAbilityRegistry().getAbilityDescription(this);
	}

	default Collection<Collider> getColliders() {
		return Collections.emptyList();
	}

	void onCollision(Collision collision);

	void recalculateConfig();
}
