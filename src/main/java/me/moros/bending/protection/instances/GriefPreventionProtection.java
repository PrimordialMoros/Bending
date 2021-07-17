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

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class GriefPreventionProtection implements Protection {
  private final GriefPrevention griefPrevention;

  public GriefPreventionProtection(@NonNull Plugin plugin) {
    griefPrevention = (GriefPrevention) plugin;
  }

  @Override
  public boolean canBuild(@NonNull LivingEntity entity, @NonNull Block block) {
    if (entity instanceof Player player) {
      String reason = griefPrevention.allowBuild(player, block.getLocation());
      Claim claim = griefPrevention.dataStore.getClaimAt(block.getLocation(), true, null);
      return reason == null || claim == null || claim.siegeData != null;
    }
    return true;
  }
}
