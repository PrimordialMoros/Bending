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

import java.util.function.BiPredicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;

/**
 * Built-in conditions to check whether a User can create an ability instance.
 */
public final class BendingConditions {
  private BendingConditions() {
  }

  /**
   * Checks if ability is on cooldown.
   */
  public static final BiPredicate<User, AbilityDescription> COOLDOWN = (u, d) -> d.bypassCooldown() || !u.onCooldown(d);
  /**
   * Checks if user has the required element.
   */
  public static final BiPredicate<User, AbilityDescription> ELEMENT = (u, d) -> u.hasElements(d.elements());
  /**
   * Checks if user is not a spectator.
   */
  public static final BiPredicate<User, AbilityDescription> GAMEMODE = (u, d) -> !u.isSpectator();
  /**
   * Checks if user has all required permissions to use the ability.
   */
  public static final BiPredicate<User, AbilityDescription> PERMISSION = (u, d) -> u.hasPermission(d);
  /**
   * Checks if user can bend (hasn't toggled bending off).
   */
  public static final BiPredicate<User, AbilityDescription> CAN_BEND = (u, d) -> u.canBend();
  /**
   * Checks if the user is in a bending enabled world.
   */
  public static final BiPredicate<User, AbilityDescription> WORLD = (u, d) -> u.game().worldManager().isEnabled(u.worldKey());

  private static final BiPredicate<User, AbilityDescription> ALL;

  static {
    ALL = Stream.of(
      COOLDOWN,
      ELEMENT,
      GAMEMODE,
      WORLD,
      CAN_BEND,
      PERMISSION
    ).reduce((u, d) -> true, BiPredicate::and);
  }

  /**
   * Convenience method to retrieve all BendingConditions.
   * @return a composite BiPredicate that combines all available BendingConditions
   */
  public static BiPredicate<User, AbilityDescription> all() {
    return ALL;
  }
}
