/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.paper.listener;

import me.moros.bending.api.game.Game;
import org.bukkit.World;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

interface BukkitListener {
  Game game();

  default boolean disabledWorld(BlockEvent event) {
    return disabledWorld(event.getBlock().getWorld());
  }

  default boolean disabledWorld(EntityEvent event) {
    return disabledWorld(event.getEntity().getWorld());
  }

  default boolean disabledWorld(PlayerEvent event) {
    return disabledWorld(event.getPlayer().getWorld());
  }

  default boolean disabledWorld(World world) {
    return !game().worldManager().isEnabled(world.key());
  }
}
