/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.fabric.listener;

import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import me.moros.bending.api.game.Game;
import me.moros.bending.common.listener.AbstractConnectionListener;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.fabric.mixin.accessor.ServerLoginPacketListenerImplAccess;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

public final class ConnectionListener extends AbstractConnectionListener implements Initializer {
  public ConnectionListener(Logger logger, Supplier<Game> gameSupplier) {
    super(logger, gameSupplier);
  }

  @Override
  public void init() {
    ServerLoginConnectionEvents.QUERY_START.register(this::onPlayerPreLogin);
    ServerPlayerEvents.JOIN.register(this::onPlayerJoin);
    ServerPlayerEvents.LEAVE.register(this::onPlayerLeave);
  }

  private void onPlayerPreLogin(ServerLoginPacketListenerImpl handler, MinecraftServer server, PacketSender sender, LoginSynchronizer synchronizer) {
    GameProfile prof = ((ServerLoginPacketListenerImplAccess) handler).bending$profile();
    if (prof != null) {
      synchronizer.waitFor(asyncJoin(prof.id()));
    }
  }

  private void onPlayerJoin(ServerPlayer player) {
    syncJoin(player.getUUID(), () -> new FabricPlayer(player));
  }

  private void onPlayerLeave(ServerPlayer player) {
    onQuit(player.getUUID());
  }
}
