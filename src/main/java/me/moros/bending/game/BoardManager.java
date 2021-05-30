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

package me.moros.bending.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages every individual {@link Board}
 */
public final class BoardManager {
  private final Map<UUID, Board> scoreboardPlayers;
  private final boolean enabled;

  BoardManager() {
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
      if (desc != null && !desc.canBind()) {
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

  private static class Board {
    private final String[] cachedSlots = new String[10];
    private final Set<String> misc = new HashSet<>(); // Stores scoreboard scores for combos and misc abilities

    private final Player player;

    private final Scoreboard bendingBoard;
    private final Objective bendingSlots;
    private int selectedSlot;

    private Board(Player player) {
      this.player = player;
      selectedSlot = player.getInventory().getHeldItemSlot() + 1;
      bendingBoard = Bukkit.getScoreboardManager().getNewScoreboard();
      bendingSlots = bendingBoard.registerNewObjective("BendingBoard", "dummy", Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
      bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
      player.setScoreboard(bendingBoard);
      Arrays.fill(cachedSlots, "");
      updateAll();
    }

    private void disableScoreboard() {
      bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
      bendingSlots.unregister();
      player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void updateSlot(int slot) {
      if (slot < 1 || slot > 9 || !player.getScoreboard().equals(bendingBoard)) {
        return;
      }
      BendingPlayer bendingPlayer = Bending.game().benderRegistry().player(player);
      String prefix = slot == selectedSlot ? ">" : "  ";

      AbilityDescription desc = bendingPlayer.boundAbility(slot).orElse(null);
      Component component;
      if (desc == null) {
        component = Message.BENDING_BOARD_EMPTY_SLOT.build(prefix, String.valueOf(slot));
      } else {
        Component name = Component.text(desc.name(), desc.element().color());
        if (bendingPlayer.onCooldown(desc)) {
          name = name.decorate(TextDecoration.STRIKETHROUGH);
        }
        component = Component.text(prefix).append(name);
      }
      String legacy = LegacyComponentSerializer.legacySection().serialize(component) + ChatColor.values()[slot].toString();
      if (!cachedSlots[slot].equals(legacy)) {
        bendingBoard.resetScores(cachedSlots[slot]);
      }
      cachedSlots[slot] = legacy;
      bendingSlots.getScore(legacy).setScore(-slot);
    }

    private void updateAll() {
      IntStream.rangeClosed(1, 9).forEach(this::updateSlot);
    }

    private void activeSlot(int oldSlot, int newSlot) {
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      updateSlot(oldSlot);
      updateSlot(newSlot);
    }

    private void updateMisc(AbilityDescription desc, boolean show) {
      Component component = Component.text("  ").append(desc.displayName().decorate(TextDecoration.STRIKETHROUGH));
      String legacy = LegacyComponentSerializer.legacySection().serialize(component);
      if (show) {
        if (misc.isEmpty()) {
          bendingSlots.getScore("  ------------  ").setScore(-10);
        }
        misc.add(legacy);
        bendingSlots.getScore(legacy).setScore(-11);
      } else {
        misc.remove(legacy);
        bendingBoard.resetScores(legacy);
        if (misc.isEmpty()) {
          bendingBoard.resetScores("  ------------  ");
        }
      }
    }
  }
}
