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

package me.moros.bending.sponge.board;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.board.Board;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.sponge.platform.entity.SpongePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;

public final class BoardImpl implements Board {
  private static final Component INACTIVE = Component.text("> ", NamedTextColor.DARK_GRAY);
  private static final Component ACTIVE = Component.text("> ");

  private final Map<AbilityDescription, IndexedTeam> misc = new ConcurrentHashMap<>(); // Used for combos and misc abilities
  private final BendingPlayer player;
  private final ServerPlayer spongePlayer;

  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;
  private int selectedSlot;

  public BoardImpl(BendingPlayer player) {
    this.player = player;
    this.spongePlayer = ((SpongePlayer) player.entity()).handle();
    selectedSlot = player.currentSlot();
    bendingBoard = Scoreboard.builder().build();
    bendingSlots = Objective.builder().name("BendingBoard").criterion(Criteria.DUMMY)
      .displayName(Message.BENDING_BOARD_TITLE.build()).objectiveDisplayMode(ObjectiveDisplayModes.INTEGER).build();
    bendingBoard.addObjective(bendingSlots);
    bendingBoard.updateDisplaySlot(bendingSlots, DisplaySlots.SIDEBAR);
    spongePlayer.setScoreboard(bendingBoard);
    updateAll();
    for (int slot = 1; slot <= 9; slot++) { // init slot prefixes
      getOrCreateTeam(slot).setPrefix(slot == selectedSlot ? ACTIVE : INACTIVE);
    }
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.clearSlot(DisplaySlots.SIDEBAR);
    bendingBoard.teams().forEach(Team::unregister);
    Sponge.game().server().serverScoreboard().ifPresent(spongePlayer::setScoreboard);
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
      getOrCreateTeam(slot).setSuffix(suffix);
    }
  }

  @Override
  public void activeSlot(int oldSlot, int newSlot) {
    if (validSlot(oldSlot) && validSlot(newSlot)) {
      if (!spongePlayer.scoreboard().equals(bendingBoard)) {
        return;
      }
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      getOrCreateTeam(oldSlot).setPrefix(INACTIVE);
      getOrCreateTeam(newSlot).setPrefix(ACTIVE);
    }
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    if (show && misc.isEmpty()) {
      bendingSlots.findOrCreateScore(Component.text(SEP)).setScore(-10);
    }
    Team team = misc.computeIfAbsent(desc, d -> createTeam(11, pickAvailableSlot())).team();
    if (show) {
      team.setPrefix(INACTIVE.append(desc.displayName().decorate(TextDecoration.STRIKETHROUGH)));
    } else {
      team.members().forEach(bendingBoard::removeScores);
      team.unregister();
      misc.remove(desc);
      if (misc.isEmpty()) {
        var score = Component.text(SEP);
        bendingSlots.findScore(score).ifPresent(s -> bendingBoard.removeScores(score));
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
    return bendingBoard.team(String.valueOf(slot)).orElseGet(() -> createTeam(slot, slot).team());
  }

  private IndexedTeam createTeam(int slot, int textSlot) {
    Team team = Team.builder().name(String.valueOf(textSlot)).build();
    bendingBoard.registerTeam(team);
    Component hidden = generateInvisibleLegacyString(textSlot);
    team.addMember(hidden);
    bendingSlots.findOrCreateScore(hidden).setScore(-slot);
    return new IndexedTeam(team, textSlot);
  }

  private record IndexedTeam(Team team, int textSlot) {
  }

  private static final Component[] CHAT_CODES;

  static {
    var arr = NamedTextColor.NAMES.values().toArray(NamedTextColor[]::new);
    CHAT_CODES = new Component[arr.length];
    for (int i = 0; i < arr.length; i++) {
      CHAT_CODES[i] = Component.text(" ", arr[i]); // TODO space might render weirdly but is needed
    }
  }

  private static Component generateInvisibleLegacyString(int slot) {
    Component hidden = CHAT_CODES[slot % CHAT_CODES.length];
    return slot <= CHAT_CODES.length ? hidden : hidden.append(generateInvisibleLegacyString(slot - CHAT_CODES.length));
  }
}
