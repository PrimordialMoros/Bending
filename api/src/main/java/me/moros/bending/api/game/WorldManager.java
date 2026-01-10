/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.game;

import java.util.UUID;
import java.util.function.Consumer;

import me.moros.bending.api.ability.Updatable;
import net.kyori.adventure.key.Key;

/**
 * Responsible for updating per-world managers.
 */
public interface WorldManager extends Updatable {
  /**
   * Get the ability manager for the specified world.
   * @param world the world to check
   * @return the ability manager instance for that world
   */
  AbilityManager instance(Key world);

  /**
   * Check if bending is enabled for the specified world.
   * @param world the world uuid to check
   * @return true if bending is enabled for the given world, false otherwise
   */
  boolean isEnabled(Key world);

  /**
   * Perform an action for every ability manager handled by this instance.
   * @param consumer the action to perform
   */
  void forEach(Consumer<AbilityManager> consumer);

  void onWorldUnload(Key world);

  void onUserChangeWorld(UUID uuid, Key oldWorld, Key newWorld);
}
