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

package me.moros.bending.sponge.platform.entity;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.item.SpongePlayerInventory;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

public class SpongePlayer extends SpongeLivingEntity implements Player {
  public SpongePlayer(ServerPlayer handle) {
    super(handle);
  }

  @Override
  public ServerPlayer handle() {
    return (ServerPlayer) super.handle();
  }

  @Override
  public boolean valid() {
    return handle().isOnline();
  }

  @Override
  public boolean isOnGround() {
    return Player.super.isOnGround();
  }

  @Override
  public PlayerInventory inventory() {
    return new SpongePlayerInventory(handle());
  }

  @Override
  public boolean hasPermission(String permission) {
    return handle().hasPermission(permission);
  }

  @Override
  public boolean canSee(Entity other) {
    return handle().canSee(PlatformAdapter.toSpongeEntity(other));
  }

  @Override
  public Audience audience() {
    return handle();
  }
}
