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

package me.moros.bending.fabric.platform.entity;

import java.util.Locale;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.fabric.platform.item.FabricPlayerInventory;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;

public class FabricPlayer extends FabricLivingEntity implements Player {
  public FabricPlayer(ServerPlayer handle) {
    super(handle);
  }

  @Override
  public ServerPlayer handle() {
    return (ServerPlayer) super.handle();
  }

  @Override
  public boolean isOnGround() {
    return Player.super.isOnGround();
  }

  @Override
  public boolean hasPermission(String permission) {
    return Permissions.check(handle(), permission, handle().serverLevel().getServer().getOperatorUserPermissionLevel());
  }

  @Override
  public Locale locale() {
    return handle().getOrDefault(Identity.LOCALE, Translation.DEFAULT_LOCALE);
  }

  @Override
  public PlayerInventory inventory() {
    return new FabricPlayerInventory(handle());
  }

  @Override
  public boolean valid() {
    return !handle().hasDisconnected();
  }

  @Override
  public boolean sneaking() {
    return handle().isShiftKeyDown();
  }

  @Override
  public void sneaking(boolean sneaking) {
    handle().setShiftKeyDown(sneaking);
  }

  @Override
  public TriState isRightHanded() {
    return TriState.byBoolean(handle().getMainArm() == HumanoidArm.RIGHT);
  }

  @Override
  public GameMode gamemode() {
    return switch (handle().gameMode.getGameModeForPlayer()) {
      case SURVIVAL -> GameMode.SURVIVAL;
      case CREATIVE -> GameMode.CREATIVE;
      case ADVENTURE -> GameMode.ADVENTURE;
      case SPECTATOR -> GameMode.SPECTATOR;
    };
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
