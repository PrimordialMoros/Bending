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

package me.moros.bending.model.predicate.removal;

import java.util.HashSet;
import java.util.Set;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

public enum Policies implements RemovalPolicy {
  DEAD((u, d) -> u.dead()),
  OFFLINE((u, d) -> !u.valid()),
  SNEAKING((u, d) -> u.sneaking()),
  NOT_SNEAKING((u, d) -> !u.sneaking()),
  IN_LIQUID((u, d) -> u.headBlock().isLiquid() || u.locBlock().isLiquid());

  private final RemovalPolicy policy;

  Policies(RemovalPolicy policy) {
    this.policy = policy;
  }

  @Override
  public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
    return policy.test(user, desc);
  }

  /**
   * Constructs a new builder that includes {@link Policies#DEAD} and {@link Policies#OFFLINE}.
   */
  public static @NonNull PolicyBuilder builder() {
    return new PolicyBuilder()
      .add(Policies.DEAD)
      .add(Policies.OFFLINE);
  }

  public static class PolicyBuilder {
    private final Set<RemovalPolicy> policies;

    private PolicyBuilder() {
      policies = new HashSet<>();
    }

    public @NonNull PolicyBuilder add(@NonNull RemovalPolicy policy) {
      policies.add(policy);
      return this;
    }

    public @NonNull PolicyBuilder remove(@NonNull RemovalPolicy policy) {
      policies.remove(policy);
      return this;
    }

    public @NonNull RemovalPolicy build() {
      return new CompositeRemovalPolicy(this);
    }
  }

  private static class CompositeRemovalPolicy implements RemovalPolicy {
    private final Set<RemovalPolicy> policies;

    private CompositeRemovalPolicy(@NonNull PolicyBuilder builder) {
      this.policies = Set.copyOf(builder.policies);
    }

    @Override
    public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
      return policies.stream().anyMatch(p -> p.test(user, desc));
    }
  }
}

