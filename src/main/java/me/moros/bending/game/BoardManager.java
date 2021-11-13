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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import io.papermc.paper.text.PaperComponents;
import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages every individual {@link Board}
 */
public final class BoardManager {
  private final Map<UUID, Board> scoreboardPlayers;
  private final boolean enabled;

  BoardManager() {
    scoreboardPlayers = new ConcurrentHashMap<>();
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

  public void invalidate(@NonNull User user) {
    scoreboardPlayers.remove(user.entity().getUniqueId());
  }

  private static class Board {
    private static final String SEP = "  ------------  ";
    private final Set<String> misc = ConcurrentHashMap.newKeySet(); // Used for combos and misc abilities
    private final BendingPlayer player;

    private final Scoreboard bendingBoard;
    private final Objective bendingSlots;
    private int selectedSlot;

    private Board(Player player) {
      this.player = Registries.BENDERS.user(player);
      selectedSlot = player.getInventory().getHeldItemSlot() + 1;
      bendingBoard = Bukkit.getScoreboardManager().getNewScoreboard();
      bendingSlots = bendingBoard.registerNewObjective("BendingBoard", "dummy", Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
      bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
      player.setScoreboard(bendingBoard);
      updateAll();
    }

    private void disableScoreboard() {
      bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
      bendingSlots.unregister();
      player.entity().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void updateSlot(int slot) {
      if (slot < 1 || slot > 9 || !player.entity().getScoreboard().equals(bendingBoard)) {
        return;
      }
      String prefix = slot == selectedSlot ? ">" : "  ";
      AbilityDescription desc = player.boundAbility(slot);
      Component component;
      if (desc == null) {
        component = Message.BENDING_BOARD_EMPTY_SLOT.build(prefix, String.valueOf(slot));
      } else {
        Component name = desc.displayName();
        if (player.onCooldown(desc)) {
          name = name.decorate(TextDecoration.STRIKETHROUGH);
        }
        component = Component.text(prefix).append(name);
      }
      getOrCreateTeam(slot).prefix(component);
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
      String legacy = PaperComponents.legacySectionSerializer().serialize(component);
      if (show) {
        if (misc.isEmpty()) {
          bendingSlots.getScore(SEP).setScore(-10);
        }
        misc.add(legacy);
        bendingSlots.getScore(legacy).setScore(-11);
      } else {
        misc.remove(legacy);
        bendingBoard.resetScores(legacy);
        if (misc.isEmpty()) {
          bendingBoard.resetScores(SEP);
        }
      }
    }

    private Team getOrCreateTeam(int slot) {
      Team team = bendingBoard.getTeam(String.valueOf(slot));
      return team == null ? createTeam(slot) : team;
    }

    private Team createTeam(int slot) {
      Team team = bendingBoard.registerNewTeam(String.valueOf(slot));
      String hidden = generateHiddenEntry(slot);
      team.addEntry(hidden);
      bendingSlots.getScore(hidden).setScore(-slot);
      return team;
    }

    private String generateHiddenEntry(int slot) {
      String hidden = ChatColor.values()[slot % 16].toString();
      return slot <= 16 ? hidden : hidden + generateHiddenEntry(slot - 16);
    }
  }
}
