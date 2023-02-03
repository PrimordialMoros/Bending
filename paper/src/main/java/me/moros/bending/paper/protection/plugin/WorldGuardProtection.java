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

package me.moros.bending.paper.protection.plugin;

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
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.protection.AbstractProtection;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.bending.paper.platform.entity.BukkitPlayer;
import org.bukkit.plugin.Plugin;

public final class WorldGuardProtection extends AbstractProtection {
  private final WorldGuard worldGuard;
  private final StateFlag bendingFlag;

  public WorldGuardProtection(Plugin plugin) {
    super(plugin.getName());
    worldGuard = WorldGuard.getInstance();
    bendingFlag = (StateFlag) worldGuard.getFlagRegistry().get("bending");
  }

  @Override
  public boolean canBuild(LivingEntity entity, Block block) {
    RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
    var w = PlatformAdapter.toBukkitWorld(block.world());
    Location location = BukkitAdapter.adapt(new org.bukkit.Location(w, block.blockX(), block.blockY(), block.blockZ()));
    StateFlag flagToCheck = bendingFlag == null ? Flags.BUILD : bendingFlag;
    if (entity instanceof BukkitPlayer player) {
      LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player.handle());
      World world = BukkitAdapter.adapt(w);
      if (worldGuard.getPlatform().getSessionManager().hasBypass(localPlayer, world)) {
        return true;
      }
      return query.testState(location, localPlayer, flagToCheck);
    }
    // Query WorldGuard to see if a non-member (entity) can build in a region.
    return query.testState(location, list -> Association.NON_MEMBER, flagToCheck);
  }
}
