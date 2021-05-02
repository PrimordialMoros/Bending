/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.protection.instances;

import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GriefPreventionProtection implements Protection {
  private final GriefPrevention griefPrevention;

  public GriefPreventionProtection() throws PluginNotFoundException {
    griefPrevention = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");
    if (griefPrevention == null) {
      throw new PluginNotFoundException("GriefPrevention");
    }
  }

  @Override
  public boolean canBuild(@NonNull User user, @NonNull Block block) {
    if (user instanceof BendingPlayer) {
      String reason = griefPrevention.allowBuild(((BendingPlayer) user).entity(), block.getLocation());
      Claim claim = griefPrevention.dataStore.getClaimAt(block.getLocation(), true, null);
      return reason == null || claim == null || claim.siegeData != null;
    }
    return true;
  }

  @Override
  public String toString() {
    return "GriefPrevention";
  }
}
