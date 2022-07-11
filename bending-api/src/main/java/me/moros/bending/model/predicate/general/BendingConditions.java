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

package me.moros.bending.model.predicate.general;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.util.metadata.Metadata;

public enum BendingConditions implements BiPredicate<User, AbilityDescription> {
  COOLDOWN((u, d) -> d.bypassCooldown() || !u.onCooldown(d)),
  ELEMENT((u, d) -> u.hasElement(d.element())),
  GAMEMODE((u, d) -> !u.spectator()),
  PERMISSION((u, d) -> u.hasPermission(d)),
  DISABLED((u, d) -> !u.entity().hasMetadata(Metadata.DISABLED)),
  WORLD((u, d) -> !u.world().hasMetadata(Metadata.DISABLED));

  private final BiPredicate<User, AbilityDescription> predicate;

  BendingConditions(BiPredicate<User, AbilityDescription> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(User user, AbilityDescription desc) {
    return predicate.test(user, desc);
  }

  private static final BiPredicate<User, AbilityDescription> ALL;

  static {
    Set<BiPredicate<User, AbilityDescription>> conditions = new HashSet<>();
    conditions.add(BendingConditions.COOLDOWN);
    conditions.add(BendingConditions.ELEMENT);
    conditions.add(BendingConditions.GAMEMODE);
    conditions.add(BendingConditions.WORLD);
    conditions.add(BendingConditions.DISABLED);
    conditions.add(BendingConditions.PERMISSION);
    ALL = conditions.stream().reduce((u, d) -> true, BiPredicate::and);
  }

  public static BiPredicate<User, AbilityDescription> all() {
    return ALL;
  }
}
