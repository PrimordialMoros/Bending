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

package me.moros.bending.platform.entity;

import java.util.Objects;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.moros.bending.platform.entity.player.GameMode;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.FabricPlayerInventory;
import me.moros.bending.platform.item.Inventory;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FabricPlayer extends FabricLivingEntity implements Player {
  public FabricPlayer(ServerPlayer handle) {
    super(handle);
  }

  @Override
  public ServerPlayer handle() {
    return (ServerPlayer) super.handle();
  }

  @Override
  public boolean hasPermission(String permission) {
    return Permissions.check(handle(), permission, handle().getLevel().getServer().getOperatorUserPermissionLevel());
  }

  @Override
  public Inventory inventory() {
    return new FabricPlayerInventory(handle());
  }

  @Override
  public boolean valid() {
    return handle().server.getPlayerList().getPlayer(uuid()) != null;
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
    var name = handle().gameMode.getGameModeForPlayer().getName();
    return Objects.requireNonNull(GameMode.registry().fromString(name));
  }

  @Override
  public boolean canSee(Entity other) {
    return true; // TODO hook into vanish mods?
  }

  @Override
  public @NonNull Audience audience() {
    return handle();
  }
}
