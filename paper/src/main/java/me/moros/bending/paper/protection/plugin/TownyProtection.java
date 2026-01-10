/*
 * Copyright 2020-2026 Moros
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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.protection.AbstractProtection;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

public final class TownyProtection extends AbstractProtection {
  private final TownyAPI api;

  public TownyProtection(Plugin plugin) {
    super(plugin.getName());
    api = TownyAPI.getInstance();
  }

  @Override
  public boolean canBuild(LivingEntity entity, Block block) {
    var loc = new Location(PlatformAdapter.toBukkitWorld(block.world()), block.blockX(), block.blockY(), block.blockZ());
    if (entity instanceof Player player) {
      var bukkitPlayer = PlatformAdapter.toBukkitEntity(player);
      return PlayerCacheUtil.getCachePermission(bukkitPlayer, loc, Material.DIRT, TownyPermission.ActionType.BUILD);
    }
    TownBlock townBlock = api.getTownBlock(loc);
    return townBlock == null || !townBlock.hasTown();
  }
}
