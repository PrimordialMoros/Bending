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

package me.moros.bending;

import me.moros.bending.model.manager.Game;

/**
 * Static singleton for conveniently accessing the {@link Game} instance for Bending.
 */
public final class GameProvider {
  private static Game INSTANCE = null;

  private GameProvider() {
  }

  /**
   * Gets an instance of the {@link Game} service.
   * @return the loaded service
   * @throws IllegalStateException if the service is not loaded
   */
  public static Game get() {
    if (INSTANCE == null) {
      throw new IllegalStateException("Bending Game Service is not loaded.");
    }
    return INSTANCE;
  }

  static void register(Game game) {
    INSTANCE = game;
  }

  static void unregister() {
    INSTANCE = null;
  }
}
