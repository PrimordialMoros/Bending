/*
 * Copyright 2020-2025 Moros
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

import me.moros.bending.api.platform.entity.EntityProperties;
import net.kyori.adventure.util.TriState;

/**
 * Built-in policies to check whether an ability needs to be removed.
 */
public final class Policies {
  private Policies() {
  }

  /**
   * Checks if the user is dead.
   */
  public static final RemovalPolicy DEAD = (u, d) -> u.propertyValue(EntityProperties.DEAD);
  /**
   * Checks if the user is invalid or disconnected.
   */
  public static final RemovalPolicy INVALID = (u, d) -> !u.valid();
  /**
   * Checks if the user is sneaking.
   */
  public static final RemovalPolicy SNEAKING = (u, d) -> u.sneaking();
  /**
   * Checks if the user is NOT sneaking.
   */
  public static final RemovalPolicy NOT_SNEAKING = (u, d) -> !u.sneaking();
  /**
   * Checks if the user is flying.
   */
  public static final RemovalPolicy FLYING = (u, d) -> u.checkProperty(EntityProperties.FLYING) == TriState.TRUE;
  /**
   * Checks if the user is submerged underwater.
   */
  public static final RemovalPolicy UNDER_WATER = (u, d) -> u.underWater();
  /**
   * Checks if the user is in water.
   */
  public static final RemovalPolicy PARTIALLY_UNDER_WATER = (u, d) -> u.inWater();
  /**
   * Checks if the user is submerged under lava.
   */
  public static final RemovalPolicy UNDER_LAVA = (u, d) -> u.underLava();
  /**
   * Checks if the user is in lava.
   */
  public static final RemovalPolicy PARTIALLY_UNDER_LAVA = (u, d) -> u.inLava();

  private static final RemovalPolicy DEFAULT = builder().build();

  /**
   * Convenience method to retrieve the default removal policy.
   * @return a composite RemovalPolicy that includes {@link Policies#DEAD} and {@link Policies#INVALID}.
   */
  public static RemovalPolicy defaults() {
    return DEFAULT;
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
