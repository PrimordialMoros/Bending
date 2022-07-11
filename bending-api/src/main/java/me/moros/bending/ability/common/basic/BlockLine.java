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

package me.moros.bending.ability.common.basic;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.bukkit.block.Block;

public abstract class BlockLine extends MovementResolver implements Updatable {
  private final User user;
  protected final Ray ray;

  protected Vector3d location;
  protected Vector3d dir;

  private final double maxRange;

  protected long interval = 0;

  protected double distanceTravelled = 0;
  private long nextUpdate = 0;

  protected BlockLine(User user, Ray ray) {
    super(user.world());
    this.user = user;
    this.ray = ray;
    this.maxRange = ray.direction.length();
    dir = ray.direction.withY(0).normalize();
    this.location = ray.origin;
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

    Vector3d newLocation = resolve(location, dir);
    if (newLocation == null) {
      return UpdateResult.REMOVE;
    }

    location = newLocation;
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
}
