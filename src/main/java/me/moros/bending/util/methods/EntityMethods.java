/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util.methods;

import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.util.collision.AABBUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class with useful {@link Entity} related methods. Note: This is not thread-safe.
 */
public final class EntityMethods {
  private EntityMethods() {
  }

  /**
   * Check if a user is against a wall made of blocks matching the given predicate.
   * <p> Note: Passable blocks and barriers are ignored.
   * @param entity the entity to check
   * @param predicate the type of blocks to accept
   * @return whether the user is against a wall
   */
  public static boolean isAgainstWall(@NonNull Entity entity, @NonNull Predicate<Block> predicate) {
    Block origin = entity.getLocation().getBlock();
    for (BlockFace face : BlockMethods.SIDES) {
      Block relative = origin.getRelative(face);
      if (relative.isPassable() || relative.getType() == Material.BARRIER) {
        continue;
      }
      if (predicate.test(relative)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Accurately checks if an entity is standing on ground using {@link AABB}.
   * @param entity the entity to check
   * @return true if entity standing on ground, false otherwise
   */
  public static boolean isOnGround(@NonNull Entity entity) {
    if (!(entity instanceof Player)) {
      return entity.isOnGround();
    }
    AABB entityBounds = AABBUtils.entityBounds(entity).grow(new Vector3d(0, 0.05, 0));
    AABB floorBounds = new AABB(new Vector3d(-1, -0.1, -1), new Vector3d(1, 0.1, 1)).at(new Vector3d(entity.getLocation()));
    for (Block block : WorldMethods.nearbyBlocks(entity.getWorld(), floorBounds, b -> !b.isPassable())) {
      if (entityBounds.intersects(AABBUtils.blockBounds(block))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Calculates the distance between an entity and the ground using precise {@link AABB} colliders.
   * By default it ignores all passable materials except liquids.
   * @param entity the entity to check
   * @return the distance in blocks between the entity and ground or the max world height.
   */
  public static double distanceAboveGround(@NonNull Entity entity) {
    int maxHeight = entity.getWorld().getMaxHeight();
    AABB entityBounds = AABBUtils.entityBounds(entity).grow(new Vector3d(0, maxHeight, 0));
    Block origin = entity.getLocation().getBlock();
    for (int i = 0; i < maxHeight; i++) {
      Block check = origin.getRelative(BlockFace.DOWN, i);
      if (check.getY() <= 0) {
        break;
      }
      AABB checkBounds = check.isLiquid() ? AABB.BLOCK_BOUNDS.at(new Vector3d(check)) : AABBUtils.blockBounds(check);
      if (checkBounds.intersects(entityBounds)) {
        return Math.max(0, entity.getBoundingBox().getMinY() - checkBounds.max.getY());
      }
    }
    return maxHeight;
  }

  /**
   * Calculates a vector at the center of the given entity using its height.
   * @param entity the entity to get the vector for
   * @return the resulting vector
   */
  public static @NonNull Vector3d entityCenter(@NonNull Entity entity) {
    return new Vector3d(entity.getLocation()).add(new Vector3d(0, entity.getHeight() / 2, 0));
  }
}
