/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.platform.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

public interface BlockGetter {
  default Block blockAt(Position position) {
    return blockAt(position.blockX(), position.blockY(), position.blockZ());
  }

  Block blockAt(int x, int y, int z);

  default BlockType getBlockType(Position position) {
    return getBlockType(position.blockX(), position.blockY(), position.blockZ());
  }

  BlockType getBlockType(int x, int y, int z);

  default BlockState getBlockState(Position position) {
    return getBlockState(position.blockX(), position.blockY(), position.blockZ());
  }

  BlockState getBlockState(int x, int y, int z);

  /**
   * Collects all blocks in a sphere.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @return all collected blocks
   * @see #nearbyBlocks(AABB)
   */
  default List<Block> nearbyBlocks(Vector3d pos, double radius) {
    return nearbyBlocks(pos, radius, block -> true, 0);
  }

  /**
   * Collects all blocks in a sphere that satisfy the given predicate.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @return all collected blocks
   * @see #nearbyBlocks(AABB, Predicate)
   */
  default List<Block> nearbyBlocks(Vector3d pos, double radius, Predicate<Block> predicate) {
    return nearbyBlocks(pos, radius, predicate, 0);
  }

  /**
   * Collects all blocks in a sphere that satisfy the given predicate.
   * <p>Note: Limit is only respected if positive. Otherwise, all blocks that satisfy the given predicate are collected.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   * @see #nearbyBlocks(AABB, Predicate, int)
   */
  default List<Block> nearbyBlocks(Vector3d pos, double radius, Predicate<Block> predicate, int limit) {
    int r = FastMath.ceil(radius) + 1;
    List<Block> blocks = new ArrayList<>();
    for (double x = pos.x() - r; x <= pos.x() + r; x++) {
      for (double y = pos.y() - r; y <= pos.y() + r; y++) {
        for (double z = pos.z() - r; z <= pos.z() + r; z++) {
          Vector3d loc = Vector3d.of(x, y, z);
          if (pos.distanceSq(loc) > radius * radius) {
            continue;
          }
          Block block = blockAt(loc);
          if (predicate.test(block)) {
            blocks.add(block);
            if (limit > 0 && blocks.size() >= limit) {
              return blocks;
            }
          }
        }
      }
    }
    return blocks;
  }

  /**
   * Collects all blocks inside a bounding box.
   * @param box the bounding box to check
   * @return all collected blocks
   * @see #nearbyBlocks(Vector3d, double)
   */
  default List<Block> nearbyBlocks(AABB box) {
    return nearbyBlocks(box, block -> true, 0);
  }

  /**
   * Collects all blocks inside a bounding box that satisfy the given predicate.
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @return all collected blocks
   * @see #nearbyBlocks(Vector3d, double, Predicate)
   */
  default List<Block> nearbyBlocks(AABB box, Predicate<Block> predicate) {
    return nearbyBlocks(box, predicate, 0);
  }

  /**
   * Collects all blocks inside a bounding box that satisfy the given predicate.
   * <p>Note: Limit is only respected if positive. Otherwise, all blocks that satisfy the given predicate are collected.
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   * @see #nearbyBlocks(Vector3d, double, Predicate, int)
   */
  default List<Block> nearbyBlocks(AABB box, Predicate<Block> predicate, int limit) {
    if (box.equals(AABB.dummy())) {
      return List.of();
    }
    List<Block> blocks = new ArrayList<>();
    for (double x = box.min().x(); x <= box.max().x(); x++) {
      for (double y = box.min().y(); y <= box.max().y(); y++) {
        for (double z = box.min().z(); z <= box.max().z(); z++) {
          Block block = blockAt(FastMath.floor(x), FastMath.floor(y), FastMath.floor(z));
          if (predicate.test(block)) {
            blocks.add(block);
            if (limit > 0 && blocks.size() >= limit) {
              return blocks;
            }
          }
        }
      }
    }
    return blocks;
  }

  default AABB blockBounds(Position position) {
    return blockBounds(position.blockX(), position.blockY(), position.blockZ());
  }

  AABB blockBounds(int x, int y, int z);

  /**
   * Search for the top block that satisfies the given predicate.
   * @param origin the position to search from
   * @param height the max height to check relative to the block
   * @param predicate the predicate to satisfy for every block
   * @return the result if found
   * @see #findBottom(Position, int, Predicate)
   */
  Optional<Block> findTop(Position origin, int height, Predicate<Block> predicate);

  /**
   * Search for the bottom block that satisfies the given predicate.
   * @param origin the position to search from
   * @param height the max height to check relative to the block
   * @param predicate the predicate to satisfy for every block
   * @return the result if found
   * @see #findTop(Position, int, Predicate)
   */
  Optional<Block> findBottom(Position origin, int height, Predicate<Block> predicate);

  default DataHolder blockMetadata(Position position) {
    return blockMetadata(position.blockX(), position.blockY(), position.blockZ());
  }

  DataHolder blockMetadata(int x, int y, int z);

  boolean isBlockEntity(Position position);

  @Nullable Lockable containerLock(Position position);
}
