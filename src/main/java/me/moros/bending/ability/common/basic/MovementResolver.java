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

package me.moros.bending.ability.common.basic;

import java.util.function.Predicate;

import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class MovementResolver {
  private final World world;
  protected Predicate<Block> diagonalsPredicate = MaterialUtil::isTransparent;

  protected MovementResolver(@NonNull World world) {
    this.world = world;
  }

  protected @Nullable Vector3 resolve(@NonNull Vector3 origin, @NonNull Vector3 direction) {
    Block original = origin.toBlock(world);
    Block destination = origin.add(direction).toBlock(world);
    int offset = 0;
    if (!isValidBlock(destination)) {
      if (isValidBlock(destination.getRelative(BlockFace.UP)) && diagonalsPredicate.test(original.getRelative(BlockFace.UP))) {
        offset = 1;
      } else if (isValidBlock(destination.getRelative(BlockFace.DOWN)) && diagonalsPredicate.test(destination)) {
        offset = -1;
      } else {
        return null;
      }
    }

    int diagonalCollisions = 0;
    for (IntVector v : VectorMethods.decomposeDiagonals(origin, direction)) {
      Block block = original.getRelative(v.x, v.y + offset, v.z);
      if (!isValidBlock(block)) {
        if (++diagonalCollisions > 1) {
          return null;
        }
      }
    }

    return origin.add(direction).add(new Vector3(0, offset, 0));
  }

  protected abstract boolean isValidBlock(@NonNull Block block);
}
