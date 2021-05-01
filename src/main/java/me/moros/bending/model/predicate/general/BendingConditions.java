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

package me.moros.bending.model.predicate.general;

import java.util.HashSet;
import java.util.Set;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

public enum BendingConditions implements BendingConditional {
  COOLDOWN((u, d) -> (!u.isOnCooldown(d))),
  ELEMENT((u, d) -> u.hasElement(d.element())),
  GAMEMODE((u, d) -> !u.spectator()),
  PERMISSION((u, d) -> u.hasPermission(d)),
  TOGGLED((u, d) -> false),
  WORLD((u, d) -> !Bending.game().isDisabledWorld(u.world().getUID()));

  private final BendingConditional predicate;

  BendingConditions(BendingConditional predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
    return predicate.test(user, desc);
  }

  /**
   * Constructs a new builder that includes {@link BendingConditions#ELEMENT}, {@link BendingConditions#WORLD},
   * {@link BendingConditions#PERMISSION} and {@link BendingConditions#GAMEMODE}.
   */
  public static @NonNull ConditionBuilder builder() {
    return new ConditionBuilder()
      .add(BendingConditions.COOLDOWN)
      .add(BendingConditions.ELEMENT)
      .add(BendingConditions.GAMEMODE)
      .add(BendingConditions.WORLD)
      .add(BendingConditions.PERMISSION);
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

    @NonNull Set<@NonNull BendingConditional> conditionals() {
      return conditionals;
    }
  }
}
