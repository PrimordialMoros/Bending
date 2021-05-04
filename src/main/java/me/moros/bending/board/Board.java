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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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

public class Board {
  private final String[] cachedSlots = new String[10];
  private final Set<String> misc = new HashSet<>(); // Stores scoreboard scores for combos and misc abilities

  private final Player player;

  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;
  private int selectedSlot;

  protected Board(Player player) {
    this.player = player;
    selectedSlot = player.getInventory().getHeldItemSlot() + 1;
    bendingBoard = Bukkit.getScoreboardManager().getNewScoreboard();
    bendingSlots = bendingBoard.registerNewObjective("BendingBoard", "dummy", Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
    bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
    player.setScoreboard(bendingBoard);
    Arrays.fill(cachedSlots, "");
    updateAll();
  }

  protected void disableScoreboard() {
    bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
    bendingSlots.unregister();
    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
  }

  protected void updateSlot(int slot) {
    if (slot < 1 || slot > 9 || !player.getScoreboard().equals(bendingBoard)) {
      return;
    }
    BendingPlayer bendingPlayer = Bending.game().playerManager().player(player);
    String prefix = slot == selectedSlot ? ">" : "  ";

    AbilityDescription desc = bendingPlayer.slotAbility(slot).orElse(null);
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

  protected void updateAll() {
    IntStream.rangeClosed(1, 9).forEach(this::updateSlot);
  }

  protected void activeSlot(int oldSlot, int newSlot) {
    if (selectedSlot != oldSlot) {
      oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
    }
    selectedSlot = newSlot;
    updateSlot(oldSlot);
    updateSlot(newSlot);
  }

  protected void updateMisc(AbilityDescription desc, boolean show) {
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
