/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.sponge.listener;

import me.moros.bending.api.game.Game;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;

public class WorldListener extends SpongeListener {
  public WorldListener(Game game) {
    super(game);
  }

  @Listener(order = Order.LAST)
  public void onWorldUnload(UnloadWorldEvent event) {
    var key = PlatformAdapter.fromRsk(event.world().key());
    game.worldManager().onWorldUnload(key);
  }

  @Listener(order = Order.LAST)
  public void onPlayerChangeWorld(ChangeEntityWorldEvent event) {
    var from = PlatformAdapter.fromRsk(event.originalWorld().key());
    var to = PlatformAdapter.fromRsk(event.destinationWorld().key());
    game.worldManager().onUserChangeWorld(event.entity().uniqueId(), from, to);
  }
}
