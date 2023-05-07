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

package me.moros.bending.api.gui;

import me.moros.bending.api.platform.entity.player.Player;

/**
 * Represents a GUI that allows a player to choose an element.
 */
public interface ElementGui {
  /**
   * Show this gui to the specified player.
   * @param player the player to show the gui to
   * @return true if the gui was correctly opened and shown to the player, false otherwise
   */
  boolean show(Player player);
}
