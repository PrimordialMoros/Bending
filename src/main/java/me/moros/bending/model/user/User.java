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

package me.moros.bending.model.user;

import java.util.Optional;

import me.moros.bending.Bending;
import me.moros.bending.game.ProtectionSystem;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface User extends BukkitUser, ElementUser {
  /**
   * Check if the user has the specified ability on cooldown.
   * @param desc the ability to check
   * @return true if the ability is on cooldown for this user, false otherwise
   */
  boolean onCooldown(@NonNull AbilityDescription desc);

  /**
   * Attempts to put the specified ability on cooldown for the given duration.
   * @param desc the ability to put on cooldown
   * @param duration the duration of the cooldown
   * @return true if cooldown was added successfully, false otherwise
   */
  boolean addCooldown(@NonNull AbilityDescription desc, long duration);

  /**
   * Makes a preset out of this user's current slots.
   * @param name the name of the preset to be created
   * @return the constructed preset
   */
  @NonNull Preset createPresetFromSlots(@NonNull String name);

  int bindPreset(@NonNull Preset preset);

  /**
   * Assigns an ability to the specified slot.
   * @param slot the slot number in the range [1, 9] (inclusive)
   * @param desc the ability to bind
   */
  void bindAbility(int slot, @Nullable AbilityDescription desc);

  /**
   * Retrieve the ability assigned to the specified slot.
   * @param slot the slot number to check, slot must be in range [1, 9] (inclusive)
   * @return the ability bound to given slot if found
   */
  Optional<AbilityDescription> boundAbility(int slot);

  /**
   * Retrives the currently selected ability for the user.
   * @return the ability in the currently selected slot for the user if found
   */
  Optional<AbilityDescription> selectedAbility();

  /**
   * @return the ability's name or an empty string if no ability is bound to the currently selected slot
   */
  default @NonNull String selectedAbilityName() {
    return selectedAbility().map(AbilityDescription::name).orElse("");
  }

  /**
   * Clears the specified slot.
   * @param slot the slot number to clear, slot must be in range [1, 9] (inclusive)
   */
  default void clearSlot(int slot) {
    bindAbility(slot, null);
  }

  /**
   * @return the bending conditional for this user
   */
  @NonNull CompositeBendingConditional bendingConditional();

  /**
   * Check whether this user can bend the specified ability.
   * @param desc the ability to check
   * @return true if the user can bend the given ability, false otherwise
   */
  default boolean canBend(@NonNull AbilityDescription desc) {
    return bendingConditional().test(this, desc);
  }

  /**
   * Checks bound abilities and clears any invalid ability slots.
   * A slot is considered invalid if the user doesn't have the ability's element or doesn't have its permission.
   */
  void validateSlots();

  /**
   * Check if the user has the specified permission.
   * This will always return true if the user is a non-player.
   * @param permission the permission to check
   * @return true if the user has the given permission, false otherwise
   */
  default boolean hasPermission(@NonNull String permission) {
    return true;
  }

  /**
   * @see #hasPermission(String)
   */
  default boolean hasPermission(@NonNull AbilityDescription desc) {
    return hasPermission(desc.permission());
  }

  /**
   * @see ProtectionSystem#canBuild(User, Block)
   */
  default boolean canBuild(@NonNull Block block) {
    return Bending.game().protectionSystem().canBuild(this, block);
  }
}
