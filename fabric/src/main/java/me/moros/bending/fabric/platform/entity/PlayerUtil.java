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

import net.minecraft.server.level.ServerPlayer;

public final class PlayerUtil {
  private PlayerUtil() {
  }

  public static void setAllowFlight(ServerPlayer player, boolean allowFlight) {
    var handler = player.getAbilities();
    if (handler.flying && !allowFlight) {
      handler.flying = false;
    }
    handler.mayfly = allowFlight;
    player.onUpdateAbilities();
  }

  public static void setFlying(ServerPlayer player, boolean flying) {
    var handler = player.getAbilities();
    boolean needsUpdate = handler.flying != flying;
    if (!handler.mayfly && flying) {
      throw new IllegalArgumentException("Player is not allowed to fly!");
    }
    handler.flying = flying;
    if (needsUpdate) {
      player.onUpdateAbilities();
    }
  }
}
