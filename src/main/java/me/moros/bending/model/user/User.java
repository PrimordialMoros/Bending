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

package me.moros.bending.model.user;

import java.util.function.Predicate;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.protection.ProtectionCache;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface User extends BukkitUser, ElementUser permits BendingUser {
  @NonNull DataHolder store();

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

  boolean bindPreset(@NonNull Preset preset);

  /**
   * Assigns an ability to the specified slot.
   * @param slot the slot number in the range [1, 9] (inclusive)
   * @param desc the ability to bind
   */
  void bindAbility(int slot, @Nullable AbilityDescription desc);

  /**
   * Retrieve the ability assigned to the specified slot.
   * @param slot the slot number to check, slot must be in range [1, 9] (inclusive)
   * @return the ability bound to given slot if found, null otherwise
   */
  @Nullable AbilityDescription boundAbility(int slot);

  /**
   * @return a slot index in the 1-9 range (inclusive)
   */
  int currentSlot();

  /**
   * Changes the currently selected slot.
   * <p> Note: This has no effect on players.
   * @param slot the slot number in the range [1, 9] (inclusive)
   */
  void currentSlot(int slot);

  /**
   * Retrieves the currently selected ability for the user.
   * @return the ability in the currently selected slot for the user if found, null otherwise
   */
  @Nullable AbilityDescription selectedAbility();

  /**
   * @return the ability's name or an empty string if no ability is bound to the currently selected slot
   */
  default @NonNull String selectedAbilityName() {
    AbilityDescription selected = selectedAbility();
    return selected == null ? "" : selected.name();
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
    return desc.permissions().stream().allMatch(this::hasPermission);
  }

  /**
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild(@NonNull Block block) {
    return ProtectionCache.INSTANCE.canBuild(this, block);
  }

  /**
   * Attempt to find a possible block source that matches the given predicate.
   * @param range the max range to check
   * @param predicate the predicate to check
   * @return the source block if one was found, null otherwise
   */
  default @Nullable Block find(double range, @NonNull Predicate<@NonNull Block> predicate) {
    BlockIterator it = new BlockIterator(entity(), Math.min(100, FastMath.ceil(range)));
    while (it.hasNext()) {
      Block block = it.next();
      if (block.getType().isAir()) {
        continue;
      }
      if (predicate.test(block) && TempBlock.isBendable(block) && canBuild(block)) {
        return block;
      }
      if (!block.isPassable()) {
        break;
      }
    }
    return null;
  }
}
