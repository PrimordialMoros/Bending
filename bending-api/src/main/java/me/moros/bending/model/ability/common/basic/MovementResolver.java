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

package me.moros.bending.model.ability.common.basic;

import java.util.function.Predicate;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public abstract class MovementResolver {
  private final World world;

  protected Predicate<Block> diagonalsPredicate = MaterialUtil::isTransparent;

  protected MovementResolver(World world) {
    this.world = world;
  }

  protected Resolved resolve(Vector3d origin, Vector3d direction) {
    Vector3d temp = origin.add(direction);
    Block original = origin.toBlock(world);
    Block destination = temp.toBlock(world);
    int offset = 0;
    if (!isValidBlock(destination)) {
      if (isValidBlock(destination.getRelative(BlockFace.UP)) && diagonalsPredicate.test(original.getRelative(BlockFace.UP))) {
        offset = 1;
      } else if (isValidBlock(destination.getRelative(BlockFace.DOWN)) && diagonalsPredicate.test(destination)) {
        offset = -1;
      } else {
        onCollision(temp);
        return new Resolved(temp, false);
      }
    }

    int diagonalCollisions = 0;
    for (Vector3i v : VectorUtil.decomposeDiagonals(origin, direction)) {
      Block block = original.getRelative(v.x(), v.y() + offset, v.z());
      if (!isValidBlock(block)) {
        if (++diagonalCollisions > 1) {
          Vector3d point = temp.add(v.x(), v.y() + offset, v.z());
          onCollision(point);
          return new Resolved(point, false);
        }
      }
    }

    return new Resolved(temp.add(0, offset, 0), true);
  }

  protected abstract boolean isValidBlock(Block block);

  protected void onCollision(Vector3d point) {
  }

  protected record Resolved(Vector3d point, boolean success) {
  }
}
