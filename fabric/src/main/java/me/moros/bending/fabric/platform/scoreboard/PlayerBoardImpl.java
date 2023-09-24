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

import java.util.Collection;
import java.util.List;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

record PlayerBoardImpl(ServerScoreboard handle) implements PlayerBoard {
  @Override
  public List<Packet<?>> getStartTrackingPackets(Objective objective) {
    return handle().getStartTrackingPackets(objective);
  }

  @Override
  public Collection<PlayerTeam> getPlayerTeams() {
    return handle().getPlayerTeams();
  }

  @Override
  public @Nullable Objective getDisplayObjective(DisplaySlot displaySlot) {
    return handle().getDisplayObjective(displaySlot);
  }
}
