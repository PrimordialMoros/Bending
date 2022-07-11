/*
 * Copyright 2020-2022 Moros
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
import org.bukkit.util.BoundingBox;

public final class AABBUtil {
  public static final AABB DUMMY_COLLIDER = new DummyCollider();

  private AABBUtil() {
  }

  /**
   * @param block the block to check
   * @return the provided block's {@link AABB} in relative space or a {@link DummyCollider} if the block has no collider
   */
  public static AABB blockDimensions(Block block) {
    return dimensions(block, Vector3d.ZERO);
  }

  /**
   * @param block the block to check
   * @return the provided block's {@link AABB} or a {@link DummyCollider} if the block has no collider
   */
  public static AABB blockBounds(Block block) {
    return dimensions(block, new Vector3d(block));
  }

  /**
   * @param entity the entity to check
   * @return the provided entity's {@link AABB} in relative space
   */
  public static AABB entityDimensions(Entity entity) {
    return dimensions(entity, Vector3d.ZERO);
  }

  /**
   * @param entity the entity to check
   * @return the provided entity's {@link AABB}
   */
  public static AABB entityBounds(Entity entity) {
    return dimensions(entity, new Vector3d(entity.getLocation()));
  }

  private static AABB dimensions(Block block, Vector3d point) {
    BoundingBox box = block.getBoundingBox();
    if (box.getVolume() == 0 || !block.isCollidable()) {
      return DUMMY_COLLIDER;
    }
    double dx = point.x() - block.getX();
    double dy = point.y() - block.getY();
    double dz = point.z() - block.getZ();
    Vector3d min = new Vector3d(box.getMinX() + dx, box.getMinY() + dy, box.getMinZ() + dz);
    Vector3d max = new Vector3d(box.getMaxX() + dx, box.getMaxY() + dy, box.getMaxZ() + dz);
    return new AABB(min, max);
  }

  private static AABB dimensions(Entity entity, Vector3d point) {
    double halfWidth = 0.5 * entity.getWidth();
    Vector3d min = new Vector3d(point.x() - halfWidth, point.y(), point.z() - halfWidth);
    Vector3d max = new Vector3d(point.x() + halfWidth, point.y() + entity.getHeight(), point.z() + halfWidth);
    return new AABB(min, max);
  }
}
