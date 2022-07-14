/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.protection.plugin;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import me.moros.bending.model.key.Key;
import me.moros.bending.model.protection.Protection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class TownyProtection implements Protection {
  private final TownyAPI api;
  private final Key key;

  public TownyProtection(Plugin plugin) {
    api = TownyAPI.getInstance();
    key = Key.create(NAMESPACE, plugin.getName());
  }

  @Override
  public boolean canBuild(LivingEntity entity, Block block) {
    if (entity instanceof Player player) {
      return PlayerCacheUtil.getCachePermission(player, block.getLocation(), Material.DIRT, TownyPermission.ActionType.BUILD);
    }
    TownBlock townBlock = api.getTownBlock(block.getLocation());
    return townBlock == null || !townBlock.hasTown();
  }

  @Override
  public Key key() {
    return key;
  }
}
