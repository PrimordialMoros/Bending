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

import me.moros.bending.model.manager.Game;
import me.moros.bending.platform.world.SpongeWorldManager;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;

public record WorldListener(Game game) {
  public WorldListener(Game game, Server server) {
    this(game);
    for (var world : server.worldManager().worlds()) {
      game.worldManager().onWorldLoad(world.properties().name(), world.uniqueId());
    }
  }

  @Listener(order = Order.LAST)
  public void onWorldLoad(LoadWorldEvent event) {
    var world = event.world();
    game.worldManager().onWorldLoad(world.properties().name(), world.uniqueId());
  }

  @Listener(order = Order.LAST)
  public void onWorldUnload(UnloadWorldEvent event) {
    var uuid = event.world().uniqueId();
    game.worldManager().onWorldUnload(uuid);
    SpongeWorldManager.INSTANCE.cleanup(uuid);
  }

  @Listener(order = Order.LAST)
  public void onPlayerChangeWorld(ChangeEntityWorldEvent event) {
    var from = event.originalWorld().uniqueId();
    var to = event.destinationWorld().uniqueId();
    game.worldManager().onUserChangeWorld(event.entity().uniqueId(), from, to);
  }
}
