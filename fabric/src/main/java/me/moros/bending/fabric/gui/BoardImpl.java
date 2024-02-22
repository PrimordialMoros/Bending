/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.fabric.gui;

import java.util.List;

import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.gui.AbstractBoard;
import me.moros.bending.common.locale.Message;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import me.moros.bending.fabric.platform.scoreboard.PlayerBoard;
import me.moros.bending.fabric.platform.scoreboard.PlayerScoreboard;
import me.moros.bending.fabric.platform.scoreboard.ScoreboardUtil;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;

public final class BoardImpl extends AbstractBoard<PlayerTeam> {
  private final FabricPlayer fabricPlayer;
  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;

  public BoardImpl(DelegatePlayer player) {
    super(Registries.BENDERS.getOrThrow(player.uuid()));
    this.fabricPlayer = (FabricPlayer) player.entity();
    bendingBoard = new PlayerScoreboard(fabricPlayer);
    var displayName = toNative(Message.BENDING_BOARD_TITLE.build());
    bendingSlots = bendingBoard.addObjective("BendingBoard", ObjectiveCriteria.DUMMY, displayName, RenderType.INTEGER, false, BlankFormat.INSTANCE);
    bendingBoard.setDisplayObjective(DisplaySlot.SIDEBAR, bendingSlots);
    ScoreboardUtil.setScoreboard(fabricPlayer.handle(), (PlayerBoard) bendingBoard);
    init();
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
    List.copyOf(bendingBoard.getPlayerTeams()).forEach(bendingBoard::removePlayerTeam);
    bendingBoard.removeObjective(bendingSlots);
    ScoreboardUtil.resetScoreboard(fabricPlayer.handle());
  }

  @Override
  protected void setPrefix(PlayerTeam playerTeam, Component content) {
    playerTeam.setPlayerPrefix(toNative(content));
  }

  @Override
  protected void setSuffix(PlayerTeam playerTeam, Component content) {
    playerTeam.setPlayerSuffix(toNative(content));
  }

  @Override
  protected PlayerTeam getOrCreateTeam(int slot) {
    PlayerTeam team = bendingBoard.getPlayerTeam(String.valueOf(slot));
    return team == null ? createTeam(slot, slot).team() : team;
  }

  @Override
  protected Indexed<PlayerTeam> createTeam(int slot, int textSlot) {
    PlayerTeam team = bendingBoard.addPlayerTeam(String.valueOf(textSlot));
    String hidden = generateInvisibleLegacyString(textSlot);
    bendingBoard.addPlayerToTeam(hidden, team);
    bendingBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(hidden), bendingSlots).set(-slot);
    return Indexed.create(team, textSlot);
  }

  @Override
  protected void removeTeam(PlayerTeam playerTeam) {
    List.copyOf(playerTeam.getPlayers()).forEach(s -> bendingBoard.resetSinglePlayerScore(ScoreHolder.forNameOnly(s), bendingSlots));
    bendingBoard.removePlayerTeam(playerTeam);
  }

  private net.minecraft.network.chat.Component toNative(Component text) {
    return FabricServerAudiences.of(fabricPlayer.handle().server).toNative(text);
  }
}
