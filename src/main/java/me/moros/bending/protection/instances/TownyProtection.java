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

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import me.moros.bending.protection.PluginNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TownyProtection implements Protection {
  private final Towny towny;
  private final TownyAPI api;

  public TownyProtection() throws PluginNotFoundException {
    towny = (Towny) Bukkit.getPluginManager().getPlugin("Towny");
    if (towny == null) {
      throw new PluginNotFoundException("Towny");
    }
    api = TownyAPI.getInstance();
  }

  @Override
  public boolean canBuild(@NonNull LivingEntity entity, @NonNull Block block) {
    if (entity instanceof Player) {
      Player player = (Player) entity;
      boolean canBuild = PlayerCacheUtil.getCachePermission(player, block.getLocation(), Material.DIRT, TownyPermission.ActionType.BUILD);
      if (!canBuild && api.isWarTime()) {
        PlayerCache cache = towny.getCache(player);
        PlayerCache.TownBlockStatus status = cache.getStatus();
        if (status == PlayerCache.TownBlockStatus.ENEMY && !WarUtil.isPlayerNeutral(player)) {
          try {
            TownyWorld townyWorld = api.getDataSource().getWorld(player.getWorld().getName());
            WorldCoord worldCoord = new WorldCoord(townyWorld.getName(), Coord.parseCoord(block));
            FlagWar.callAttackCellEvent(towny, player, block, worldCoord);
          } catch (Exception e) {
            // Do nothing
          }
          canBuild = true;
        } else if (status == PlayerCache.TownBlockStatus.WARZONE) {
          canBuild = true;
        }
      }
      return canBuild;
    }
    TownBlock townBlock = api.getTownBlock(block.getLocation());
    return townBlock == null || !townBlock.hasTown();
  }

  @Override
  public String toString() {
    return "Towny";
  }
}
