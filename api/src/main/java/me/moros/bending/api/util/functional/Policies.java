/*
 * Copyright 2020-2024 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.api.util.functional;

import java.util.HashSet;
import java.util.Set;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.platform.property.EntityProperty;
import me.moros.bending.api.user.User;
import net.kyori.adventure.util.TriState;

/**
 * Built-in policies to check whether an ability needs to be removed.
 */
public enum Policies implements RemovalPolicy {
  /**
   * Checks if the user is dead.
   */
  DEAD((u, d) -> u.dead()),
  /**
   * Checks if the user is invalid or disconnected.
   */
  INVALID((u, d) -> !u.valid()),
  /**
   * Checks if the user is sneaking.
   */
  SNEAKING((u, d) -> u.sneaking()),
  /**
   * Checks if the user is NOT sneaking.
   */
  NOT_SNEAKING((u, d) -> !u.sneaking()),
  /**
   * Checks if the user is flying.
   */
  FLYING((u, d) -> u.checkProperty(EntityProperty.FLYING) == TriState.TRUE),
  /**
   * Checks if the user is submerged underwater.
   */
  UNDER_WATER((u, d) -> u.underWater()),
  /**
   * Checks if the user is in water.
   */
  PARTIALLY_UNDER_WATER((u, d) -> u.inWater()),
  /**
   * Checks if the user is submerged under lava.
   */
  UNDER_LAVA((u, d) -> u.underLava()),
  /**
   * Checks if the user is in lava.
   */
  PARTIALLY_UNDER_LAVA((u, d) -> u.inLava());

  private final RemovalPolicy policy;

  Policies(RemovalPolicy policy) {
    this.policy = policy;
  }

  @Override
  public boolean test(User user, AbilityDescription desc) {
    return policy.test(user, desc);
  }

  /**
   * Constructs a new builder that includes {@link Policies#DEAD} and {@link Policies#INVALID}.
   */
  public static Builder builder() {
    return new Builder()
      .add(Policies.DEAD)
      .add(Policies.INVALID);
  }

  /**
   * Builder to create complex {@link RemovalPolicy}.
   */
  public static final class Builder {
    private final Set<RemovalPolicy> policies;

    private Builder() {
      policies = new HashSet<>();
    }

    public Builder add(RemovalPolicy policy) {
      policies.add(policy);
      return this;
    }

    public Builder remove(RemovalPolicy policy) {
      policies.remove(policy);
      return this;
    }

    public RemovalPolicy build() {
      return policies.stream().reduce((u, d) -> false, RemovalPolicy::or);
    }
  }
}

