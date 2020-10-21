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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;

import java.util.Collection;
import java.util.Collections;

public interface Ability extends Updatable {
	boolean activate(@NonNull User user, @NonNull ActivationMethod method); // return true if the ability was activated

	void onDestroy();

	@NonNull User getUser();

	@NonNull String getName();

	default boolean setUser(@NonNull User newUser) {
		return false;
	}

	default boolean isStatic() {
		return false;
	}

	default @NonNull AbilityDescription getDescription() {
		return Bending.getGame().getAbilityRegistry().getAbilityDescription(this);
	}

	default @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.emptyList();
	}

	void onCollision(@NonNull Collision collision);

	void recalculateConfig();
}
