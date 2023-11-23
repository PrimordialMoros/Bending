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

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.protection.AbstractProtection;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.plugin.Plugin;

public final class LWCProtection extends AbstractProtection {
  private final LWC lwc;

  public LWCProtection(Plugin plugin) {
    super(plugin.getName());
    lwc = ((LWCPlugin) plugin).getLWC();
  }

  @Override
  public boolean canBuild(LivingEntity entity, Block block) {
    if (entity instanceof Player player) {
      var b = PlatformAdapter.toBukkitWorld(block.world()).getBlockAt(block.blockX(), block.blockY(), block.blockZ());
      Protection protection = lwc.getProtectionCache().getProtection(b);
      return protection == null || lwc.canAccessProtection(PlatformAdapter.toBukkitEntity(player), protection);
    }
    return true;
  }
}
