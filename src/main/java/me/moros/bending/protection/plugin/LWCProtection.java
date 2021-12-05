/*
 * Copyright 2020-2021 Moros
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

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import me.moros.bending.protection.Protection;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class LWCProtection implements Protection {
  private final LWC lwc;

  public LWCProtection(@NonNull Plugin plugin) {
    lwc = ((LWCPlugin) plugin).getLWC();
  }

  @Override
  public boolean canBuild(@NonNull LivingEntity entity, @NonNull Block block) {
    if (entity instanceof Player player) {
      com.griefcraft.model.Protection protection = lwc.getProtectionCache().getProtection(block);
      return protection == null || lwc.canAccessProtection(player, protection);
    }
    return true;
  }
}
