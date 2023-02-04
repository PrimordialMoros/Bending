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

package me.moros.bending.fabric.listener;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.authlib.GameProfile;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.User;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import me.moros.bending.common.BendingPlugin;
import me.moros.bending.fabric.mixin.accessor.ServerLoginPacketListenerImplAccess;
import me.moros.bending.fabric.platform.entity.FabricPlayer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

public record ConnectionListener(Supplier<Game> gameSupplier, BendingPlugin plugin,
                                 AsyncLoadingCache<UUID, PlayerBenderProfile> profileCache) implements FabricListener {
  public ConnectionListener(Supplier<Game> gameSupplier, BendingPlugin plugin, BendingStorage storage) {
    this(gameSupplier, plugin, createCache(storage));
    ServerLoginConnectionEvents.QUERY_START.register(this::onPlayerPreLogin);
    ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerLogout);
  }

  private static AsyncLoadingCache<UUID, PlayerBenderProfile> createCache(BendingStorage storage) {
    return Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2))
      .buildAsync(storage::createProfile);
  }

  private void onPlayerPreLogin(ServerLoginPacketListenerImpl handler, MinecraftServer server, PacketSender sender, LoginSynchronizer synchronizer) {
    GameProfile prof = ((ServerLoginPacketListenerImplAccess) handler).profile();
    if (prof != null) {
      synchronizer.waitFor(profileOrTimeout(prof.getId()));
    }
  }

  private CompletableFuture<?> profileOrTimeout(UUID uuid) {
    long startTime = System.currentTimeMillis();
    CompletableFuture<PlayerBenderProfile> future = profileCache.get(uuid).orTimeout(1000, TimeUnit.MILLISECONDS);
    return future.thenApply(profile -> {
      long deltaTime = System.currentTimeMillis() - startTime;
      if (profile != null && deltaTime > 500) {
        plugin.logger().warn("Processing login for " + uuid + " took " + deltaTime + "ms.");
      }
      return profile;
    }).exceptionally(t -> {
      if (t instanceof TimeoutException) {
        plugin.logger().warn("Timed out while retrieving data for " + uuid);
      } else {
        plugin.logger().warn(t.getMessage(), t);
      }
      return null;
    });
  }

  private void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
    ServerPlayer player = handler.getPlayer();
    UUID uuid = player.getUUID();
    PlayerBenderProfile profile = profileCache.synchronous().get(uuid);
    if (profile != null) {
      User user = User.create(game(), new FabricPlayer(player), profile).orElse(null);
      if (user != null) {
        Registries.BENDERS.register(user);
        game().abilityManager(user.worldKey()).createPassives(user);
      }
    } else {
      plugin.logger().error("Could not create bending profile for: " + uuid + " (" + player.getGameProfile().getName() + ")");
    }
  }

  private void onPlayerLogout(ServerGamePacketListenerImpl handler, MinecraftServer server) {
    UUID uuid = handler.getPlayer().getUUID();
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game().activationController().onUserDeconstruct(user);
    }
    profileCache.synchronous().invalidate(uuid);
  }
}
