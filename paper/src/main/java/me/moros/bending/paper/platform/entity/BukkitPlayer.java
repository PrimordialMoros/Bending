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

package me.moros.bending.paper.platform.entity;

import java.util.Locale;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.bending.paper.platform.item.BukkitPlayerInventory;
import net.kyori.adventure.util.TriState;
import org.bukkit.inventory.MainHand;

public class BukkitPlayer extends BukkitLivingEntity implements Player {
  public BukkitPlayer(org.bukkit.entity.Player handle) {
    super(handle);
  }

  @Override
  public org.bukkit.entity.Player handle() {
    return (org.bukkit.entity.Player) super.handle();
  }

  @Override
  public boolean isOnGround() {
    return Player.super.isOnGround();
  }

  @Override
  public boolean hasPermission(String permission) {
    return handle().hasPermission(permission);
  }

  @Override
  public Locale locale() {
    return handle().locale();
  }

  @Override
  public PlayerInventory inventory() {
    return new BukkitPlayerInventory(handle());
  }

  @Override
  public boolean valid() {
    return handle().isConnected();
  }

  @Override
  public boolean sneaking() {
    return handle().isSneaking();
  }

  @Override
  public void sneaking(boolean sneaking) {
    handle().setSneaking(sneaking);
  }

  @Override
  public TriState isRightHanded() {
    return TriState.byBoolean(handle().getMainHand() == MainHand.RIGHT);
  }

  @Override
  public GameMode gamemode() {
    return switch (handle().getGameMode()) {
      case SURVIVAL -> GameMode.SURVIVAL;
      case CREATIVE -> GameMode.CREATIVE;
      case ADVENTURE -> GameMode.ADVENTURE;
      case SPECTATOR -> GameMode.SPECTATOR;
    };
  }

  @Override
  public boolean canSee(Entity other) {
    return handle().canSee(PlatformAdapter.toBukkitEntity(other));
  }
}
