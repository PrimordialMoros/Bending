/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.model.predicate.removal;

import java.util.HashSet;
import java.util.Set;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;

public enum Policies implements RemovalPolicy {
  DEAD((u, d) -> u.dead()),
  OFFLINE((u, d) -> !u.valid()),
  SNEAKING((u, d) -> u.sneaking()),
  NOT_SNEAKING((u, d) -> !u.sneaking()),
  FLYING((u, d) -> u.flying()),
  UNDER_WATER((u, d) -> EntityUtil.underWater(u.entity())),
  PARTIALLY_UNDER_WATER((u, d) -> u.entity().isInWaterOrBubbleColumn()),
  UNDER_LAVA((u, d) -> EntityUtil.underLava(u.entity())),
  PARTIALLY_UNDER_LAVA((u, d) -> u.entity().isInLava());

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
  public static Builder builder() {
    return new Builder()
      .add(Policies.DEAD)
      .add(Policies.OFFLINE);
  }

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

