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

package me.moros.bending.model.manager;

import java.util.function.Consumer;

import me.moros.bending.model.ability.Updatable;
import org.bukkit.World;

/**
 * Responsible for updating per-world managers.
 */
public interface WorldManager extends Updatable {
  /**
   * Get the ability manager for the specified world.
   * @param world the world to check
   * @return the ability manager instance for that world
   */
  AbilityManager instance(World world);

  /**
   * Clear all per-world managers this instance is responsible for.
   */
  void clear();

  /**
   * Check if bending is enabled for the specified world.
   * @param world the world to check
   * @return true if bending is enabled for the given world, false otherwise
   */
  boolean isEnabled(World world);

  /**
   * Perform an action for every ability manager handled by this instance.
   * @param consumer the action to perform
   */
  void forEach(Consumer<AbilityManager> consumer);
}
