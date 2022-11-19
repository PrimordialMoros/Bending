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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.user.User;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;

public abstract class BlockLine extends MovementResolver implements Updatable {
  private final User user;
  private final Iterator<Vector2i> iterator;

  private final double maxRange;

  protected final Ray ray;

  protected Vector3d location;
  protected Vector3d dir;

  protected long interval = 0;
  protected double distanceTravelled = 0;
  protected boolean diagonalMovement = true;

  private long nextUpdate = 0;

  protected BlockLine(User user, Ray ray) {
    super(user.world());
    this.user = user;
    this.ray = ray;
    this.maxRange = ray.direction.length();
    dir = ray.direction.withY(0).normalize();
    this.location = ray.origin;
    Collection<Vector2i> vectors = new ArrayList<>();
    new BlockIterator(user.world(), location.toBukkitVector(), dir.toBukkitVector(), 0, FastMath.ceil(maxRange))
      .forEachRemaining(b -> vectors.add(new Vector2i(b.getX(), b.getZ())));
    iterator = vectors.iterator();
  }

  @Override
  public UpdateResult update() {
    if (interval >= 50) {
      long time = System.currentTimeMillis();
      if (time < nextUpdate) {
        return UpdateResult.CONTINUE;
      }
      nextUpdate = time + interval;
    }
    Vector3d temp = location;
    if (!diagonalMovement) {
      if (!iterator.hasNext()) {
        return UpdateResult.REMOVE;
      }
      Vector2i v = iterator.next();
      temp = Vector3d.of(v.x() + 0.5, FastMath.floor(location.y() + 0.5), v.z() + 0.5);
    }
    Resolved resolved = resolve(temp, dir);
    if (!resolved.success()) {
      return UpdateResult.REMOVE;
    }
    location = resolved.point();
    Block block = location.toBlock(user.world());

    if (location.distanceSq(ray.origin) > maxRange * maxRange) {
      return UpdateResult.REMOVE;
    }

    if (!user.canBuild(block)) {
      return UpdateResult.REMOVE;
    }

    if (++distanceTravelled > 1) {
      render(block);
    }
    return UpdateResult.CONTINUE;
  }

  public abstract void render(Block block);

  protected record Vector2i(int x, int z) {
    public static Vector2i at(double x, double z) {
      return new Vector2i(FastMath.floor(x), FastMath.floor(z));
    }
  }
}
