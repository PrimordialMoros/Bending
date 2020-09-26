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
import java.util.Objects;
import java.util.Set;

public class CompositeRemovalPolicy implements RemovalPolicy {
	private final Set<RemovalPolicy> policies;

	private CompositeRemovalPolicy(CompositePolicyBuilder builder) {
		Objects.requireNonNull(builder);
		this.policies = builder.policies;
	}

	@Override
	public boolean test(User user, AbilityDescription desc) {
		return policies.stream().anyMatch(p -> p.test(user, desc));
	}

	public boolean add(RemovalPolicy policy) {
		return policies.add(policy);
	}

	public boolean remove(RemovalPolicy policy) {
		return policies.remove(policy);
	}

	public static CompositePolicyBuilder builder() {
		return new CompositePolicyBuilder();
	}

	/**
	 * Basic composite removal policy to ensure abilities are removed if a user is dead or offline
	 */
	public static CompositePolicyBuilder defaults() {
		return new CompositePolicyBuilder()
			.add(Policies.DEAD)
			.add(Policies.OFFLINE);
	}

	public static class CompositePolicyBuilder {
		private final Set<RemovalPolicy> policies;

		private CompositePolicyBuilder() {
			policies = new HashSet<>();
		}

		public CompositePolicyBuilder add(RemovalPolicy policy) {
			policies.add(policy);
			return this;
		}

		public CompositePolicyBuilder remove(RemovalPolicy policy) {
			policies.add(policy);
			return this;
		}

		public CompositeRemovalPolicy build() {
			return new CompositeRemovalPolicy(this);
		}
	}
}
