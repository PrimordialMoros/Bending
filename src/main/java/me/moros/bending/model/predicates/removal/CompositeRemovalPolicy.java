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

package me.moros.bending.model.predicates.removal;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicates.removal.Policies.PolicyBuilder;
import me.moros.bending.model.user.User;

import java.util.Set;

public class CompositeRemovalPolicy implements RemovalPolicy {
	private final Set<RemovalPolicy> policies;

	CompositeRemovalPolicy(@NonNull PolicyBuilder builder) {
		this.policies = builder.getPolicies();
	}

	@Override
	public boolean test(User user, AbilityDescription desc) {
		if (user == null || desc == null) return true;
		return policies.stream().anyMatch(p -> p.test(user, desc));
	}
}
