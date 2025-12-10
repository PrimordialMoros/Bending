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

package me.moros.bending.fabric.platform.entity;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.fabric.platform.item.FabricPlayerInventory;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlayer extends FabricLivingEntity implements Player {
  public FabricPlayer(ServerPlayer handle) {
    super(handle);
  }

  @Override
  public ServerPlayer handle() {
    return (ServerPlayer) super.handle();
  }

  @Override
  public boolean valid() {
    return !handle().hasDisconnected();
  }

  @Override
  public boolean isOnGround() {
    return Player.super.isOnGround();
  }

  @Override
  public PlayerInventory inventory() {
    return new FabricPlayerInventory(handle());
  }

  @Override
  public boolean hasPermission(String permission) {
    return Permissions.check(handle(), permission, handle().level().getServer().operatorUserPermissions().level());
  }

  @Override
  public boolean canSee(Entity other) {
    return true; // TODO hook into vanish mods?
  }

  @Override
  public Audience audience() {
    return handle();
  }
}
