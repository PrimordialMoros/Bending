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

package me.moros.bending.model.board;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

final class BoardImpl implements Board {
  private static final Component INACTIVE = Component.text("> ", NamedTextColor.DARK_GRAY);
  private static final Component ACTIVE = Component.text("> ");

  private final Map<AbilityDescription, IndexedTeam> misc = new ConcurrentHashMap<>(); // Used for combos and misc abilities
  private final BendingPlayer player;

  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;
  private int selectedSlot;

  BoardImpl(BendingPlayer player) {
    this.player = player;
    selectedSlot = player.inventory().getHeldItemSlot() + 1;
    bendingBoard = player.entity().getServer().getScoreboardManager().getNewScoreboard();
    bendingSlots = bendingBoard.registerNewObjective("BendingBoard", "dummy", Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
    bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
    player.entity().setScoreboard(bendingBoard);
    updateAll();
    for (int slot = 1; slot <= 9; slot++) { // init slot prefixes
      getOrCreateTeam(slot).prefix(slot == selectedSlot ? ACTIVE : INACTIVE);
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
    player.entity().setScoreboard(player.entity().getServer().getScoreboardManager().getMainScoreboard());
  }

  @Override
  public void updateAll() {
    for (int slot = 1; slot <= 9; slot++) {
      AbilityDescription desc = player.boundAbility(slot);
      Component suffix;
      if (desc == null) {
        suffix = Message.BENDING_BOARD_EMPTY_SLOT.build(slot);
      } else {
        suffix = desc.displayName();
        if (player.onCooldown(desc)) {
          suffix = suffix.decorate(TextDecoration.STRIKETHROUGH);
        }
      }
      getOrCreateTeam(slot).suffix(suffix);
    }
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
      getOrCreateTeam(oldSlot).prefix(INACTIVE);
      getOrCreateTeam(newSlot).prefix(ACTIVE);
    }
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    if (show && misc.isEmpty()) {
      bendingSlots.getScore(SEP).setScore(-10);
    }
    Team team = misc.computeIfAbsent(desc, d -> createTeam(11, pickAvailableSlot())).team();
    if (show) {
      team.prefix(INACTIVE.append(desc.displayName().decorate(TextDecoration.STRIKETHROUGH)));
    } else {
      team.getEntries().forEach(bendingBoard::resetScores);
      team.unregister();
      misc.remove(desc);
      if (misc.isEmpty()) {
        bendingBoard.resetScores(SEP);
      }
    }
  }

  private int pickAvailableSlot() {
    int idx = 11;
    for (IndexedTeam indexedTeam : misc.values()) {
      idx = Math.max(indexedTeam.textSlot() + 1, idx);
    }
    return idx;
  }

  private boolean validSlot(int slot) {
    return 1 <= slot && slot <= 9;
  }

  private Team getOrCreateTeam(int slot) {
    Team team = bendingBoard.getTeam(String.valueOf(slot));
    return team == null ? createTeam(slot, slot).team() : team;
  }

  private IndexedTeam createTeam(int slot, int textSlot) {
    Team team = bendingBoard.registerNewTeam(String.valueOf(textSlot));
    String hidden = TextUtil.generateInvisibleLegacyString(textSlot);
    team.addEntry(hidden);
    bendingSlots.getScore(hidden).setScore(-slot);
    return new IndexedTeam(team, textSlot);
  }

  private record IndexedTeam(Team team, int textSlot) {
  }

  static final class DummyBoard implements Board {
    static final Board INSTANCE = new DummyBoard();

    private DummyBoard() {
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public void disableScoreboard() {
    }

    @Override
    public void updateAll() {
    }

    @Override
    public void activeSlot(int oldSlot, int newSlot) {
    }

    @Override
    public void updateMisc(AbilityDescription desc, boolean show) {
    }
  }
}
