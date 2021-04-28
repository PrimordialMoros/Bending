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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;

public abstract class AbstractBlockLine implements Updatable {
  private final User user;
  protected final Ray ray;

  protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparent(b);
  protected Vector3 location;
  protected Vector3 dir;

  private final double maxRange;

  protected long interval = 0;

  private long nextUpdate;

  public AbstractBlockLine(@NonNull User user, @NonNull Ray ray) {
    this.user = user;
    this.ray = ray;
    this.maxRange = ray.direction.getNormSq();
    dir = ray.direction.normalize().setY(0);
    this.location = ray.origin.add(dir);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (interval >= 50) {
      long time = System.currentTimeMillis();
      if (time <= nextUpdate) {
        return UpdateResult.CONTINUE;
      }
      nextUpdate = time + interval;
    }

    Vector3 originalVector = new Vector3(location.toArray());
    location = location.add(dir);
    Block block = location.toBlock(user.getWorld());

    if (!isValidBlock(block)) {
      if (isValidBlock(block.getRelative(BlockFace.UP))) {
        location = location.add(Vector3.PLUS_J);
        block = block.getRelative(BlockFace.UP);
      } else if (isValidBlock(block.getRelative(BlockFace.DOWN))) {
        location = location.add(Vector3.MINUS_J);
        block = block.getRelative(BlockFace.DOWN);
      } else {
        return UpdateResult.REMOVE;
      }
    }

    if (location.distanceSq(ray.origin) > maxRange) {
      return UpdateResult.REMOVE;
    }

    Block originBlock = originalVector.toBlock(user.getWorld());
    for (Vector3 v : VectorMethods.decomposeDiagonals(originalVector, dir)) {
      int x = NumberConversions.floor(v.getX());
      int y = NumberConversions.floor(v.getY());
      int z = NumberConversions.floor(v.getZ());
      if (diagonalsPredicate.test(originBlock.getRelative(x, y, z))) {
        return UpdateResult.REMOVE;
      }
    }

    if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
      return UpdateResult.REMOVE;
    }

    render(block);

    return UpdateResult.CONTINUE;
  }

  public abstract boolean isValidBlock(@NonNull Block block);

  public abstract void render(@NonNull Block block);
}
