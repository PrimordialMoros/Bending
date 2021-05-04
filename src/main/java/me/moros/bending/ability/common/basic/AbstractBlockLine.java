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

import me.moros.bending.Bending;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractBlockLine extends AbstractTerrainFollower implements Updatable {
  private final User user;
  protected final Ray ray;

  protected Vector3 location;
  protected Vector3 dir;

  private final double maxRange;

  protected long interval = 0;

  private double distanceTravelled = 0;
  private long nextUpdate = 0;

  public AbstractBlockLine(@NonNull User user, @NonNull Ray ray) {
    this.user = user;
    this.ray = ray;
    this.maxRange = ray.direction.getNorm();
    dir = ray.direction.setY(0).normalize();
    this.location = ray.origin;
  }

  @Override
  public @NonNull UpdateResult update() {
    if (interval >= 50) {
      long time = System.currentTimeMillis();
      if (time < nextUpdate) {
        return UpdateResult.CONTINUE;
      }
      nextUpdate = time + interval;
    }

    Vector3 newLocation = resolveMovement(user.world(), location, dir);
    if (newLocation == null) {
      return UpdateResult.REMOVE;
    }

    location = newLocation;
    Block block = location.toBlock(user.world());

    if (location.distanceSq(ray.origin) > maxRange * maxRange) {
      return UpdateResult.REMOVE;
    }

    if (!Bending.game().protectionSystem().canBuild(user, block)) {
      return UpdateResult.REMOVE;
    }

    if (++distanceTravelled > 1) {
      render(block);
    }

    return UpdateResult.CONTINUE;
  }

  public abstract void render(@NonNull Block block);
}
