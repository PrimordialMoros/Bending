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

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

import java.util.HashSet;
import java.util.Set;

public enum Policies implements RemovalPolicy {
	DEAD((u, d) -> u.isDead()),
	OFFLINE((u, d) -> !u.isValid()),
	SNEAKING((u, d) -> u.isSneaking()),
	NOT_SNEAKING((u, d) -> !u.isSneaking()),
	IN_LIQUID((u, d) -> u.getHeadBlock().isLiquid() || u.getLocBlock().isLiquid());

	private final RemovalPolicy policy;

	Policies(RemovalPolicy policy) {
		this.policy = policy;
	}

	@Override
	public boolean test(User user, AbilityDescription desc) {
		return policy.test(user, desc);
	}

	/**
	 * Constructs a new builder that includes {@link Policies#DEAD} and {@link Policies#OFFLINE}.
	 */
	public static PolicyBuilder builder() {
		return new PolicyBuilder()
			.add(Policies.DEAD)
			.add(Policies.OFFLINE);
	}

	public static class PolicyBuilder {
		private final Set<RemovalPolicy> policies;

		private PolicyBuilder() {
			policies = new HashSet<>();
		}

		public PolicyBuilder add(RemovalPolicy policy) {
			policies.add(policy);
			return this;
		}

		public PolicyBuilder remove(RemovalPolicy policy) {
			policies.add(policy);
			return this;
		}

		public RemovalPolicy build() {
			return new CompositeRemovalPolicy(this);
		}

		Set<RemovalPolicy> getPolicies() {
			return policies;
		}
	}
}

