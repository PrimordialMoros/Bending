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

package me.moros.bending.model.predicates.conditionals;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

import java.util.HashSet;
import java.util.Set;

public enum BendingConditions implements BendingConditional {
	COOLDOWN((u, d) -> (!u.isOnCooldown(d))),
	ELEMENT((u, d) -> u.hasElement(d.getElement())),
	PERMISSION((u, d) -> u.hasPermission(d)),
	WORLD((u, d) -> !Bending.getGame().isDisabledWorld(u.getWorld().getUID())),
	TOGGLED((u, d) -> false),
	GAMEMODE((u, d) -> !u.isSpectator());

	private final BendingConditional predicate;

	BendingConditions(BendingConditional predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(User user, AbilityDescription desc) {
		return predicate.test(user, desc);
	}

	/**
	 * Constructs a new builder that includes {@link BendingConditions#ELEMENT}, {@link BendingConditions#WORLD},
	 * {@link BendingConditions#PERMISSION} and {@link BendingConditions#GAMEMODE}.
	 */
	public static @NonNull ConditionBuilder builder() {
		return new ConditionBuilder().add(BendingConditions.COOLDOWN)
			.add(BendingConditions.ELEMENT)
			.add(BendingConditions.WORLD)
			.add(BendingConditions.PERMISSION)
			.add(BendingConditions.GAMEMODE);
	}

	public static class ConditionBuilder {
		private final Set<BendingConditional> conditionals;

		private ConditionBuilder() {
			conditionals = new HashSet<>();
		}

		public @NonNull ConditionBuilder add(@NonNull BendingConditional conditional) {
			conditionals.add(conditional);
			return this;
		}

		public @NonNull ConditionBuilder remove(@NonNull BendingConditional conditional) {
			conditionals.remove(conditional);
			return this;
		}

		public @NonNull CompositeBendingConditional build() {
			return new CompositeBendingConditional(this);
		}

		@NonNull Set<@NonNull BendingConditional> getConditionals() {
			return conditionals;
		}
	}
}