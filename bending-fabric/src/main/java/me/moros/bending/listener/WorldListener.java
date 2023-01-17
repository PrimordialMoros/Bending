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
import me.moros.bending.platform.FabricMetadata;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public record WorldListener(Game game) {
  public WorldListener(Game game) {
    this.game = game;
    var early = new ResourceLocation("bending", "early");
    ServerWorldEvents.UNLOAD.register(this::onWorldUnload);
    ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register(early, this::onChangeWorld);
  }

  public void onWorldUnload(MinecraftServer server, ServerLevel world) {
    var key = world.dimension().location();
    game.worldManager().onWorldUnload(key);
    FabricMetadata.INSTANCE.cleanup(key);
  }

  public void onChangeWorld(Entity originalEntity, Entity newEntity, ServerLevel origin, ServerLevel destination) {
    var from = origin.dimension().location();
    var to = destination.dimension().location();
    game.worldManager().onUserChangeWorld(newEntity.getUUID(), from, to); // TODO check uuid change?
  }
}
