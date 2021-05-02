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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WorldGuardProtection implements Protection {
  private final WorldGuard worldGuard;
  private final StateFlag bendingFlag;

  public WorldGuardProtection() throws PluginNotFoundException {
    if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
      throw new PluginNotFoundException("WorldGuard");
    }
    worldGuard = WorldGuard.getInstance();
    bendingFlag = (StateFlag) worldGuard.getFlagRegistry().get("bending");
  }

  @Override
  public boolean canBuild(@NonNull User user, @NonNull Block block) {
    RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
    Location location = BukkitAdapter.adapt(block.getLocation());
    if (user instanceof BendingPlayer) {
      LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(((BendingPlayer) user).entity());
      World world = BukkitAdapter.adapt(block.getWorld());
      if (worldGuard.getPlatform().getSessionManager().hasBypass(player, world)) {
        return true;
      }
      if (bendingFlag != null && query.testState(location, player, bendingFlag)) {
        return true;
      }
      return query.testState(location, player, Flags.BUILD);
    }
    // Query WorldGuard to see if a non-member (entity) can build in a region.
    return query.testState(location, list -> Association.NON_MEMBER, Flags.BUILD);
  }

  @Override
  public String toString() {
    return "WorldGuard";
  }
}
