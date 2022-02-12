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
import me.moros.bending.util.internal.NMSUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class AABBUtil {
  public static final DummyCollider DUMMY_COLLIDER = new DummyCollider();

  private AABBUtil() {
  }

  /**
   * @param block the block to check
   * @return the provided block's {@link AABB} in relative space or a {@link DummyCollider} if the block has no collider
   */
  public static @NonNull AABB blockDimensions(@NonNull Block block) {
    return NMSUtil.dimensions(block, Vector3d.ZERO);
  }

  /**
   * @param block the block to check
   * @return the provided block's {@link AABB} or a {@link DummyCollider} if the block has no collider
   */
  public static @NonNull AABB blockBounds(@NonNull Block block) {
    return NMSUtil.dimensions(block, new Vector3d(block));
  }

  /**
   * @param entity the entity to check
   * @return the provided entity's {@link AABB} in relative space
   */
  public static @NonNull AABB entityDimensions(@NonNull Entity entity) {
    return NMSUtil.dimensions(entity, Vector3d.ZERO);
  }

  /**
   * @param entity the entity to check
   * @return the provided entity's {@link AABB}
   */
  public static @NonNull AABB entityBounds(@NonNull Entity entity) {
    return NMSUtil.dimensions(entity, new Vector3d(entity.getLocation()));
  }
}
