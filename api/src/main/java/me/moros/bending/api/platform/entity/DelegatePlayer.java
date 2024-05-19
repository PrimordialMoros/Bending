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

package me.moros.bending.api.platform.entity;

import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.PlayerInventory;

/**
 * Represents a platform player.
 */
public interface DelegatePlayer extends DelegateLivingEntity, Player {
  @Override
  Player entity();

  @Override
  default boolean hasPermission(String permission) {
    return entity().hasPermission(permission);
  }

  @Override
  default PlayerInventory inventory() {
    return entity().inventory();
  }

  @Override
  default boolean isOnGround() {
    return entity().isOnGround();
  }

  @Override
  default boolean canSee(Entity other) {
    return entity().canSee(other);
  }
}
