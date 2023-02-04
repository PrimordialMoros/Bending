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

import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.board.AbstractBoard;
import me.moros.bending.sponge.platform.entity.SpongePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.criteria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;

public final class BoardImpl extends AbstractBoard<Team> {
  private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

  private final ServerPlayer spongePlayer;
  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;

  public BoardImpl(BendingPlayer user) {
    super(user);
    this.spongePlayer = ((SpongePlayer) user.entity()).handle();
    bendingBoard = Scoreboard.builder().build();
    bendingSlots = Objective.builder().name("BendingBoard").criterion(Criteria.DUMMY)
      .displayName(Message.BENDING_BOARD_TITLE.build()).objectiveDisplayMode(ObjectiveDisplayModes.INTEGER).build();
    bendingBoard.addObjective(bendingSlots);
    bendingBoard.updateDisplaySlot(bendingSlots, DisplaySlots.SIDEBAR);
    spongePlayer.setScoreboard(bendingBoard);
    init();
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.clearSlot(DisplaySlots.SIDEBAR);
    bendingBoard.teams().forEach(Team::unregister);
    Sponge.game().server().serverScoreboard().ifPresent(spongePlayer::setScoreboard);
  }

  @Override
  protected void setPrefix(Team team, Component content) {
    team.setPrefix(content);
  }

  @Override
  protected void setSuffix(Team team, Component content) {
    team.setSuffix(content);
  }

  @Override
  protected Team getOrCreateTeam(int slot) {
    return bendingBoard.team(String.valueOf(slot)).orElseGet(() -> createTeam(slot, slot).team());
  }

  @Override
  protected Indexed<Team> createTeam(int slot, int textSlot) {
    Team team = Team.builder().name(String.valueOf(textSlot)).build();
    bendingBoard.registerTeam(team);
    Component hidden = SERIALIZER.deserialize(generateInvisibleLegacyString(textSlot));
    team.addMember(hidden);
    bendingSlots.findOrCreateScore(hidden).setScore(-slot);
    return Indexed.create(team, textSlot);
  }

  @Override
  protected void removeTeam(Team team) {
    team.members().forEach(bendingBoard::removeScores);
    team.unregister();
  }
}
