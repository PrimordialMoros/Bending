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

package me.moros.bending.model.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

final class BoardImpl implements Board {
  static final Board DUMMY = new DummyBoard();

  private final Map<AbilityDescription, Team> misc = new ConcurrentHashMap<>(); // Used for combos and misc abilities
  private final BendingPlayer player;

  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;
  private int selectedSlot;

  BoardImpl(BendingPlayer player) {
    this.player = player;
    selectedSlot = player.inventory().getHeldItemSlot() + 1;
    bendingBoard = Bukkit.getScoreboardManager().getNewScoreboard();
    bendingSlots = bendingBoard.registerNewObjective("BendingBoard", "dummy", Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
    bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
    player.entity().setScoreboard(bendingBoard);
    for (int slot = 1; slot <= 9; slot++) { // init slots
      updateSlot(slot);
      getOrCreateTeam(slot).prefix(Component.text(slot == selectedSlot ? ">" : "  "));
    }
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
    bendingBoard.getTeams().forEach(Team::unregister);
    bendingSlots.unregister();
    player.entity().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
  }

  @Override
  public void updateSlot(int slot) {
    AbilityDescription desc = player.boundAbility(slot);
    Component suffix;
    if (desc == null) {
      suffix = Message.BENDING_BOARD_EMPTY_SLOT.build(String.valueOf(slot));
    } else {
      suffix = desc.displayName();
      if (player.onCooldown(desc)) {
        suffix = suffix.decorate(TextDecoration.STRIKETHROUGH);
      }
    }
    getOrCreateTeam(slot).suffix(suffix);
  }

  @Override
  public void updateAll() {
    IntStream.rangeClosed(1, 9).forEach(this::updateSlot);
  }

  @Override
  public void activeSlot(int oldSlot, int newSlot) {
    if (validSlot(oldSlot) && validSlot(newSlot)) {
      if (!player.entity().getScoreboard().equals(bendingBoard)) {
        return;
      }
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      getOrCreateTeam(oldSlot).prefix(Component.text("  "));
      getOrCreateTeam(newSlot).prefix(Component.text(">"));
    }
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    if (show && misc.isEmpty()) {
      bendingSlots.getScore(SEP).setScore(-10);
    }
    String id = Long.toHexString(System.nanoTime());
    Team team = misc.computeIfAbsent(desc, d -> createTeam(11, id));
    if (show) {
      team.prefix(Component.text("  ").append(desc.displayName().decorate(TextDecoration.STRIKETHROUGH)));
    } else {
      team.getEntries().forEach(bendingBoard::resetScores);
      team.unregister();
      misc.remove(desc);
      if (misc.isEmpty()) {
        bendingBoard.resetScores(SEP);
      }
    }
  }

  private boolean validSlot(int slot) {
    return 1 <= slot && slot <= 9;
  }

  private Team getOrCreateTeam(int slot) {
    Team team = bendingBoard.getTeam(String.valueOf(slot));
    return team == null ? createTeam(slot, String.valueOf(slot)) : team;
  }

  private Team createTeam(int slot, String id) {
    Team team = bendingBoard.registerNewTeam(id);
    String hidden = TextUtil.generateInvisibleLegacyString(slot);
    team.addEntry(hidden);
    bendingSlots.getScore(hidden).setScore(-slot);
    return team;
  }

  private static final class DummyBoard implements Board {
    private DummyBoard() {
    }
  }
}
