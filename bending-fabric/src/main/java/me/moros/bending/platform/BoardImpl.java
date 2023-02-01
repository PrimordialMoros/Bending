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

package me.moros.bending.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.FabricBending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.platform.entity.FabricPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;

final class BoardImpl implements Board {
  private static final Component INACTIVE = Component.text("> ", NamedTextColor.DARK_GRAY);
  private static final Component ACTIVE = Component.text("> ");

  private final Map<AbilityDescription, IndexedTeam> misc = new ConcurrentHashMap<>(); // Used for combos and misc abilities
  private final BendingPlayer player;
  private final ServerPlayer fabricPlayer;

  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;
  private int selectedSlot;

  BoardImpl(BendingPlayer player) {
    this.player = player;
    this.fabricPlayer = ((FabricPlayer) player.entity()).handle();
    selectedSlot = player.currentSlot();
    bendingBoard = new ServerScoreboard(fabricPlayer.server);
    var displayName = toNative(Message.BENDING_BOARD_TITLE.build());
    bendingSlots = bendingBoard.addObjective("BendingBoard", ObjectiveCriteria.DUMMY, displayName, RenderType.INTEGER);
    bendingBoard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, bendingSlots);
    ScoreboardUtil.setScoreboard(fabricPlayer, (ServerScoreboard) bendingBoard);
    updateAll();
    for (int slot = 1; slot <= 9; slot++) { // init slot prefixes
      getOrCreateTeam(slot).setPlayerPrefix(toNative(slot == selectedSlot ? ACTIVE : INACTIVE));
    }
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, null);
    List.copyOf(bendingBoard.getPlayerTeams()).forEach(bendingBoard::removePlayerTeam);
    bendingBoard.removeObjective(bendingSlots);
    ScoreboardUtil.resetScoreboard(fabricPlayer);
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
      getOrCreateTeam(slot).setPlayerSuffix(toNative(suffix));
    }
  }

  @Override
  public void activeSlot(int oldSlot, int newSlot) {
    if (validSlot(oldSlot) && validSlot(newSlot)) {
      if (ScoreboardUtil.getScoreboard(fabricPlayer) != bendingBoard) {
        return;
      }
      if (selectedSlot != oldSlot) {
        oldSlot = selectedSlot; // Fixes bug when slot is set using setHeldItemSlot
      }
      selectedSlot = newSlot;
      getOrCreateTeam(oldSlot).setPlayerPrefix(toNative(INACTIVE));
      getOrCreateTeam(newSlot).setPlayerPrefix(toNative(ACTIVE));
    }
  }

  @Override
  public void updateMisc(AbilityDescription desc, boolean show) {
    if (show && misc.isEmpty()) {
      bendingBoard.getOrCreatePlayerScore(SEP, bendingSlots).setScore(-10);
    }
    PlayerTeam team = misc.computeIfAbsent(desc, d -> createTeam(11, pickAvailableSlot())).team();
    if (show) {
      team.setPlayerPrefix(toNative(INACTIVE.append(desc.displayName().decorate(TextDecoration.STRIKETHROUGH))));
    } else {
      List.copyOf(team.getPlayers()).forEach(s -> bendingBoard.resetPlayerScore(s, bendingSlots));
      bendingBoard.removePlayerTeam(team);
      misc.remove(desc);
      if (misc.isEmpty()) {
        bendingBoard.resetPlayerScore(SEP, bendingSlots);
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

  private PlayerTeam getOrCreateTeam(int slot) {
    PlayerTeam team = bendingBoard.getPlayerTeam(String.valueOf(slot));
    return team == null ? createTeam(slot, slot).team() : team;
  }

  private IndexedTeam createTeam(int slot, int textSlot) {
    PlayerTeam team = bendingBoard.addPlayerTeam(String.valueOf(textSlot));
    String hidden = generateInvisibleLegacyString(textSlot);
    bendingBoard.addPlayerToTeam(hidden, team);
    bendingBoard.getOrCreatePlayerScore(hidden, bendingSlots).setScore(-slot);
    return new IndexedTeam(team, textSlot);
  }

  private record IndexedTeam(PlayerTeam team, int textSlot) {
  }

  private static final String[] CHAT_CODES;

  static {
    CHAT_CODES = new String[16];
    for (int i = 0; i < CHAT_CODES.length; i++) {
      CHAT_CODES[i] = String.format("§%s§r", Integer.toHexString(i));
    }
  }

  private static String generateInvisibleLegacyString(int slot) {
    String hidden = CHAT_CODES[slot % CHAT_CODES.length];
    return slot <= CHAT_CODES.length ? hidden : hidden + generateInvisibleLegacyString(slot - CHAT_CODES.length);
  }

  private static net.minecraft.network.chat.Component toNative(Component text) {
    return FabricBending.audiences().toNative(text);
  }
}
