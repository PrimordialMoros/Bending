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

package me.moros.bending.api.platform.entity.player;

import java.util.List;
import java.util.Locale;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.Item;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;

public interface Player extends LivingEntity {
  boolean hasPermission(String permission);

  Locale locale();

  @Override
  Inventory inventory();

  /**
   * Accurately checks if this player is standing on ground using {@link AABB}.
   * @return true if entity standing on ground, false otherwise
   */
  @Override
  default boolean isOnGround() {
    AABB entityBounds = bounds().grow(Vector3d.of(0, 0.05, 0));
    AABB floorBounds = AABB.of(Vector3d.of(-1, -0.1, -1), Vector3d.of(1, 0.1, 1)).at(location());
    for (Block block : world().nearbyBlocks(floorBounds, b -> b.type().isCollidable())) {
      if (entityBounds.intersects(block.bounds())) {
        return true;
      }
    }
    return false;
  }

  GameMode gamemode();

  boolean canSee(Entity other);

  default void sendNotification(Item item, Component title) {
    Platform.instance().nativeAdapter().createNotification(item, title).send(List.of(uuid()));
  }
}
