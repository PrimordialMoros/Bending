/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.model.predicate;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;

/**
 * Built-in conditions to check whether a User can create an ability instance.
 */
public enum BendingConditions implements BiPredicate<User, AbilityDescription> {
  /**
   * Checks if ability is on cooldown.
   */
  COOLDOWN((u, d) -> d.bypassCooldown() || !u.onCooldown(d)),
  /**
   * Checks if user has the required element.
   */
  ELEMENT((u, d) -> u.hasElement(d.element())),
  /**
   * Checks if user is not a spectator.
   */
  GAMEMODE((u, d) -> !u.isSpectator()),
  /**
   * Checks if user has all required permissions to use the ability.
   */
  PERMISSION((u, d) -> u.hasPermission(d)),
  /**
   * Checks if user can bend (hasn't toggled bending off).
   */
  CAN_BEND((u, d) -> u.canBend()),
  /**
   * Checks if the user is in a bending enabled world.
   */
  WORLD((u, d) -> u.game().worldManager().isEnabled(u.worldKey()));

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
    conditions.add(BendingConditions.CAN_BEND);
    conditions.add(BendingConditions.PERMISSION);
    ALL = conditions.stream().reduce((u, d) -> true, BiPredicate::and);
  }

  /**
   * Convenience method to retrieve all BendingConditions.
   * @return a composite BiPredicate that combines all available BendingConditions
   */
  public static BiPredicate<User, AbilityDescription> all() {
    return ALL;
  }
}
