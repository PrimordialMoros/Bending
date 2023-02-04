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

package me.moros.bending.paper.board;

import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.board.AbstractBoard;
import me.moros.bending.paper.platform.entity.BukkitPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class BoardImpl extends AbstractBoard<Team> {
  private final Player bukkitPlayer;
  private final Scoreboard bendingBoard;
  private final Objective bendingSlots;

  public BoardImpl(BendingPlayer user) {
    super(user);
    this.bukkitPlayer = ((BukkitPlayer) user.entity()).handle();
    bendingBoard = bukkitPlayer.getServer().getScoreboardManager().getNewScoreboard();
    bendingSlots = bendingBoard.registerNewObjective("BendingBoard", Criteria.DUMMY, Message.BENDING_BOARD_TITLE.build(), RenderType.INTEGER);
    bendingSlots.setDisplaySlot(DisplaySlot.SIDEBAR);
    bukkitPlayer.setScoreboard(bendingBoard);
    init();
  }

  @Override
  public void disableScoreboard() {
    bendingBoard.clearSlot(DisplaySlot.SIDEBAR);
    bendingBoard.getTeams().forEach(Team::unregister);
    bendingSlots.unregister();
    bukkitPlayer.setScoreboard(bukkitPlayer.getServer().getScoreboardManager().getMainScoreboard());
  }

  @Override
  protected void setPrefix(Team team, Component content) {
    team.prefix(content);
  }

  @Override
  protected void setSuffix(Team team, Component content) {
    team.suffix(content);
  }

  @Override
  protected Team getOrCreateTeam(int slot) {
    Team team = bendingBoard.getTeam(String.valueOf(slot));
    return team == null ? createTeam(slot, slot).team() : team;
  }

  @Override
  protected Indexed<Team> createTeam(int slot, int textSlot) {
    Team team = bendingBoard.registerNewTeam(String.valueOf(textSlot));
    String hidden = generateInvisibleLegacyString(textSlot);
    team.addEntry(hidden);
    bendingSlots.getScore(hidden).setScore(-slot);
    return Indexed.create(team, textSlot);
  }

  @Override
  protected void removeTeam(Team team) {
    team.getEntries().forEach(bendingBoard::resetScores);
    team.unregister();
  }
}
