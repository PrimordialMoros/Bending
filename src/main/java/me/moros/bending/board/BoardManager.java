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

package me.moros.bending.board;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages every individual {@link Board}
 */
public final class BoardManager {
  private final Map<UUID, Board> scoreboardPlayers;
  private final boolean enabled;

  public BoardManager() {
    scoreboardPlayers = new HashMap<>();
    enabled = Bending.configManager().config().node("properties", "bending-board").getBoolean(true);
  }

  /**
   * Force toggle the scoreboard for when a player changes worlds (for example when teleporting to a world where bending is disabled)
   * @param player the player to force toggle
   */
  public void forceToggleScoreboard(@NonNull Player player) {
    if (Bending.game().isDisabledWorld(player.getWorld().getUID())) {
      UUID uuid = player.getUniqueId();
      if (scoreboardPlayers.containsKey(uuid)) {
        scoreboardPlayers.get(uuid).disableScoreboard();
        scoreboardPlayers.remove(uuid);
      }
    } else {
      canUseScoreboard(player);
    }
  }

  public boolean toggleScoreboard(@NonNull Player player) {
    if (!enabled || Bending.game().isDisabledWorld(player.getWorld().getUID())) {
      return false;
    }
    UUID uuid = player.getUniqueId();
    if (scoreboardPlayers.containsKey(uuid)) {
      scoreboardPlayers.get(uuid).disableScoreboard();
      scoreboardPlayers.remove(uuid);
      return false;
    } else {
      return canUseScoreboard(player);
    }
  }

  /**
   * Checks if a player can use the bending board and creates a BendingBoardInstance if possible.
   * @param player the player to check
   * @return true if player can use the bending board, false otherwise
   */
  public boolean canUseScoreboard(@NonNull Player player) {
    if (!enabled || Bending.game().isDisabledWorld(player.getWorld().getUID())) {
      return false;
    }
    UUID uuid = player.getUniqueId();
    if (!scoreboardPlayers.containsKey(uuid)) {
      scoreboardPlayers.put(uuid, new Board(player));
    }
    return true;
  }

  public void updateBoard(@NonNull Player player) {
    if (canUseScoreboard(player)) {
      scoreboardPlayers.get(player.getUniqueId()).updateAll();
    }
  }

  public void updateBoardSlot(@NonNull Player player, @Nullable AbilityDescription desc) {
    updateBoardSlot(player, desc, false);
  }

  public void updateBoardSlot(@NonNull Player player, @Nullable AbilityDescription desc, boolean cooldown) {
    if (canUseScoreboard(player)) {
      if (desc != null && desc.isActivatedBy(ActivationMethod.SEQUENCE)) {
        scoreboardPlayers.get(player.getUniqueId()).updateMisc(desc, cooldown);
      } else {
        scoreboardPlayers.get(player.getUniqueId()).updateAll();
      }
    }
  }

  public void changeActiveSlot(@NonNull Player player, int oldSlot, int newSlot) {
    if (canUseScoreboard(player)) {
      scoreboardPlayers.get(player.getUniqueId()).activeSlot(++oldSlot, ++newSlot);
    }
  }

  public void invalidate(@NonNull UUID uuid) {
    scoreboardPlayers.remove(uuid);
  }
}
