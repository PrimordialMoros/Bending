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

package me.moros.bending.model.board;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.platform.Platform;
import me.moros.bending.util.KeyUtil;

/**
 * Represents a bending board that utilizes a scoreboard to render bound abilities and cooldowns.
 */
public interface Board {
  DataKey<Board> HIDDEN = KeyUtil.data("hidden-board", Board.class);

  String SEP = " -------------- ";

  /**
   * Check if this instance is enabled.
   * @return true if this board is visible, false otherwise
   */
  boolean isEnabled();

  /**
   * Disable this instance.
   */
  void disableScoreboard();

  /**
   * Update all slots in this instance.
   */
  void updateAll();

  /**
   * Change the active slot for this instance.
   * @param oldSlot the previous slot in the range [1, 9] (inclusive)
   * @param newSlot the new slot in the range [1, 9] (inclusive)
   */
  void activeSlot(int oldSlot, int newSlot);

  /**
   * Update rendering for sequences and other misc abilities in this instance.
   * @param desc the ability to update
   * @param show whether to show or hide the given description
   */
  void updateMisc(AbilityDescription desc, boolean show);

  /**
   * Create a board instance for the specified player.
   * @param player the player to make a board for
   * @return a new board instance.
   */
  static Board create(BendingPlayer player) {
    if (player.hasPermission("bending.board")) {
      return Platform.instance().factory().buildBoard(player).orElseGet(Board::dummy);
    }
    return dummy();
  }

  /**
   * Get a dummy board instance.
   * @return the dummy instance
   */
  static Board dummy() {
    return DummyBoard.INSTANCE;
  }
}
