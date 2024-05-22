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

package me.moros.bending.sponge.listener;

import me.moros.bending.api.game.Game;
import me.moros.bending.common.listener.AbstractConnectionListener;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.sponge.platform.entity.SpongePlayer;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Tristate;

public final class ConnectionListener extends AbstractConnectionListener {
  public ConnectionListener(Logger logger, Game game) {
    super(logger, game);
  }

  @Listener(order = Order.EARLY)
  @IsCancelled(Tristate.UNDEFINED)
  public void onPlayerPreLogin(ServerSideConnectionEvent.Auth event) {
    asyncJoin(event.profile().uniqueId());
  }

  @Listener(order = Order.EARLY)
  public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
    ServerPlayer player = event.player();
    syncJoin(player.uniqueId(), () -> new SpongePlayer(player));
  }

  @Listener(order = Order.EARLY)
  public void onPlayerLogout(ServerSideConnectionEvent.Disconnect event) {
    event.profile().map(GameProfile::uuid).ifPresent(this::onQuit);
  }
}
