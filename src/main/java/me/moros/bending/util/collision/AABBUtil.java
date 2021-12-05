/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.util.collision;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.DummyCollider;
import me.moros.bending.model.math.Vector3d;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class AABBUtil {
  public static final DummyCollider DUMMY_COLLIDER = new DummyCollider();

  private AABBUtil() {
  }

  /**
   * @param block the block to check
   * @return the provided block's {@link AABB} or a {@link DummyCollider} if the block is passable
   */
  public static @NonNull AABB blockBounds(@NonNull Block block) {
    if (block.isPassable()) {
      return DUMMY_COLLIDER;
    }
    return new AABB(new Vector3d(block.getBoundingBox().getMin()), new Vector3d(block.getBoundingBox().getMax()));
  }

  /**
   * @param entity the entity to check
   * @return the provided entity's {@link AABB}
   */
  public static @NonNull AABB entityBounds(@NonNull Entity entity) {
    return new AABB(new Vector3d(entity.getBoundingBox().getMin()), new Vector3d(entity.getBoundingBox().getMax()));
  }
}
