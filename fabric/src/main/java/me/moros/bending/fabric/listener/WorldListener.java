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

package me.moros.bending.fabric.listener;

import java.util.UUID;
import java.util.function.Supplier;

import me.moros.bending.api.game.Game;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.fabric.platform.FabricMetadata;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record WorldListener(Supplier<Game> gameSupplier) implements FabricListener, Initializer {
  @Override
  public void init() {
    var early = ResourceLocation.fromNamespaceAndPath("bending", "early");
    ServerWorldEvents.UNLOAD.register(this::onWorldUnload);
    ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(early, this::onChangeWorld);
    ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.addPhaseOrdering(early, Event.DEFAULT_PHASE);
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(early, this::onChangeWorld);
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.addPhaseOrdering(early, Event.DEFAULT_PHASE);
  }

  private void onWorldUnload(MinecraftServer server, ServerLevel world) {
    var key = world.dimension().location();
    game().worldManager().onWorldUnload(key);
    FabricMetadata.INSTANCE.cleanup(key);
  }

  private void onChangeWorld(Entity originalEntity, Entity newEntity, ServerLevel origin, ServerLevel destination) {
    var uuid = newEntity.getUUID();
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      PlatformAdapter.toFabricEntityWrapper(user).setHandle(newEntity);
      onChangeWorld(uuid, origin, destination);
    }
  }

  private void onChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
    onChangeWorld(player.getUUID(), origin, destination);
  }

  private void onChangeWorld(UUID uuid, ServerLevel origin, ServerLevel destination) {
    var from = origin.dimension().location();
    var to = destination.dimension().location();
    game().worldManager().onUserChangeWorld(uuid, from, to);
  }
}
