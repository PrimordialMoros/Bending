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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

public class ScoreboardUtil {
  private static final Map<UUID, PlayerBoard> playerBoards = new HashMap<>();

  public static void resetScoreboard(ServerPlayer player) {
    setScoreboard(player, PlayerBoard.from(player.server.getScoreboard()));
  }

  public static void setScoreboard(ServerPlayer player, PlayerBoard scoreboard) {
    var global = PlayerBoard.from(player.server.getScoreboard());
    PlayerBoard previous = playerBoards.getOrDefault(player.getUUID(), global);
    if (scoreboard == previous) {
      return;
    }
    if (scoreboard == global) {
      playerBoards.remove(player.getUUID());
    } else {
      playerBoards.put(player.getUUID(), scoreboard);
    }

    Set<Objective> temp = new HashSet<>();

    // Remove old
    for (int i = 0; i < 3; i++) {
      Objective obj = previous.getDisplayObjective(DisplaySlot.BY_ID.apply(i));
      if (obj != null && temp.add(obj)) {
        player.connection.send(new ClientboundSetObjectivePacket(obj, 1));
      }
    }
    for (var team : previous.getPlayerTeams()) {
      player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
    }

    temp.clear();

    // Add new
    for (var team : scoreboard.getPlayerTeams()) {
      player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
    }
    for (DisplaySlot displaySlot : DisplaySlot.values()) {
      Objective obj = scoreboard.getDisplayObjective(displaySlot);
      if (obj != null && temp.add(obj)) {
        scoreboard.getStartTrackingPackets(obj).forEach(player.connection::send);
      }
    }
  }
}
