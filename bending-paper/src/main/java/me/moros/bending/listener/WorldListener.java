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

package me.moros.bending.listener;

import me.moros.bending.Bending;
import me.moros.bending.model.manager.Game;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {
  private final Game game;

  public WorldListener(Bending plugin, Game game) {
    this.game = game;
    for (World world : plugin.getServer().getWorlds()) {
      game.worldManager().onWorldLoad(world.getName(), world.getUID());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldLoad(WorldLoadEvent event) {
    World world = event.getWorld();
    game.worldManager().onWorldLoad(world.getName(), world.getUID());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    game.worldManager().onWorldUnload(event.getWorld().getUID());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
    Player p = event.getPlayer();
    game.worldManager().onUserChangeWorld(p.getUniqueId(), event.getFrom().getUID(), p.getWorld().getUID());
  }
}
