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

package me.moros.bending.api.addon;

import me.moros.bending.api.game.Game;

/**
 * Represents an addon that can be loaded.
 */
public interface Addon {
  /**
   * Called when this addon is first loaded but before a {@link Game} instance is created.
   */
  void load();

  /**
   * Called when a game instance has been created and initialized.
   * @param game the initialized game instance
   */
  default void enable(Game game) {
  }

  /**
   * Called when an addon is unloaded.
   */
  default void unload() {
  }
}
