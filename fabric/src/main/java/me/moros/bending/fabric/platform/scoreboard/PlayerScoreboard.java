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

package me.moros.bending.fabric.platform.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard.Method;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerScoreboard extends Scoreboard implements PlayerBoard {
  private final FabricPlayer player;
  private final Set<Objective> trackedObjectives = Sets.newHashSet();

  public PlayerScoreboard(FabricPlayer player) {
    this.player = player;
  }

  private void broadcast(Packet<?> packet) {
    this.player.handle().connection.send(packet);
  }

  private void broadcast(Iterable<Packet<?>> packets) {
    var conn = player.handle().connection;
    for (Packet<?> packet : packets) {
      conn.send(packet);
    }
  }

  @Override
  public Collection<PlayerTeam> getPlayerTeams() {
    return super.getPlayerTeams();
  }

  @Override
  public @Nullable Objective getDisplayObjective(int i) {
    return super.getDisplayObjective(i);
  }

  @Override
  public void onScoreChanged(Score score) {
    super.onScoreChanged(score);
    if (this.trackedObjectives.contains(score.getObjective())) {
      broadcast(new ClientboundSetScorePacket(Method.CHANGE, score.getObjective().getName(), score.getOwner(), score.getScore()));
    }
  }

  @Override
  public void onPlayerRemoved(String string) {
    super.onPlayerRemoved(string);
    broadcast(new ClientboundSetScorePacket(Method.REMOVE, null, string, 0));
  }

  @Override
  public void onPlayerScoreRemoved(String string, Objective objective) {
    super.onPlayerScoreRemoved(string, objective);
    if (this.trackedObjectives.contains(objective)) {
      broadcast(new ClientboundSetScorePacket(Method.REMOVE, objective.getName(), string, 0));
    }
  }

  @Override
  public void setDisplayObjective(int i, @Nullable Objective objective) {
    Objective objective2 = this.getDisplayObjective(i);
    super.setDisplayObjective(i, objective);
    if (objective2 != objective && objective2 != null) {
      if (this.getObjectiveDisplaySlotCount(objective2) > 0) {
        broadcast(new ClientboundSetDisplayObjectivePacket(i, objective));
      } else {
        this.stopTrackingObjective(objective2);
      }
    }
    if (objective != null) {
      if (this.trackedObjectives.contains(objective)) {
        broadcast(new ClientboundSetDisplayObjectivePacket(i, objective));
      } else {
        this.startTrackingObjective(objective);
      }
    }
  }

  @Override
  public boolean addPlayerToTeam(String string, PlayerTeam playerTeam) {
    if (super.addPlayerToTeam(string, playerTeam)) {
      broadcast(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, string, ClientboundSetPlayerTeamPacket.Action.ADD));
      return true;
    }
    return false;
  }

  @Override
  public void removePlayerFromTeam(String string, PlayerTeam playerTeam) {
    super.removePlayerFromTeam(string, playerTeam);
    broadcast(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, string, ClientboundSetPlayerTeamPacket.Action.REMOVE));
  }

  @Override
  public void onObjectiveAdded(Objective objective) {
    super.onObjectiveAdded(objective);
  }

  @Override
  public void onObjectiveChanged(Objective objective) {
    super.onObjectiveChanged(objective);
    if (this.trackedObjectives.contains(objective)) {
      broadcast(new ClientboundSetObjectivePacket(objective, 2));
    }
  }

  @Override
  public void onObjectiveRemoved(Objective objective) {
    super.onObjectiveRemoved(objective);
    if (this.trackedObjectives.contains(objective)) {
      this.stopTrackingObjective(objective);
    }
  }

  @Override
  public void onTeamAdded(PlayerTeam playerTeam) {
    super.onTeamAdded(playerTeam);
    broadcast(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
  }

  @Override
  public void onTeamChanged(PlayerTeam playerTeam) {
    super.onTeamChanged(playerTeam);
    broadcast(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, false));
  }

  @Override
  public void onTeamRemoved(PlayerTeam playerTeam) {
    super.onTeamRemoved(playerTeam);
    broadcast(ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
  }

  @Override
  public List<Packet<?>> getStartTrackingPackets(Objective objective) {
    ArrayList<Packet<?>> list = Lists.newArrayList();
    list.add(new ClientboundSetObjectivePacket(objective, 0));
    for (int i = 0; i < 19; ++i) {
      if (this.getDisplayObjective(i) != objective) continue;
      list.add(new ClientboundSetDisplayObjectivePacket(i, objective));
    }
    for (Score score : this.getPlayerScores(objective)) {
      list.add(new ClientboundSetScorePacket(Method.CHANGE, score.getObjective().getName(), score.getOwner(), score.getScore()));
    }
    return list;
  }

  public void startTrackingObjective(Objective objective) {
    broadcast(getStartTrackingPackets(objective));
    this.trackedObjectives.add(objective);
  }

  public List<Packet<?>> getStopTrackingPackets(Objective objective) {
    ArrayList<Packet<?>> list = Lists.newArrayList();
    list.add(new ClientboundSetObjectivePacket(objective, 1));
    for (int i = 0; i < 19; ++i) {
      if (this.getDisplayObjective(i) != objective) continue;
      list.add(new ClientboundSetDisplayObjectivePacket(i, objective));
    }
    return list;
  }

  public void stopTrackingObjective(Objective objective) {
    broadcast(getStopTrackingPackets(objective));
    this.trackedObjectives.remove(objective);
  }

  public int getObjectiveDisplaySlotCount(Objective objective) {
    int i = 0;
    for (int j = 0; j < 19; ++j) {
      if (this.getDisplayObjective(j) != objective) continue;
      ++i;
    }
    return i;
  }
}
