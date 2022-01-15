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

import java.util.function.Predicate;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractRide implements Updatable {
  private final User user;
  private final HeightSmoother smoother;

  private final double speed;
  private final double targetHeight;
  private int stuckCount = 0;

  protected Predicate<Block> predicate = x -> true;

  protected AbstractRide(@NonNull User user, double speed, double targetHeight) {
    this.user = user;
    this.speed = speed;
    this.targetHeight = targetHeight;
    smoother = new HeightSmoother();
  }

  @Override
  public @NonNull UpdateResult update() {
    stuckCount = user.velocity().lengthSq() < 0.1 ? stuckCount + 1 : 0;
    if (stuckCount > 10 || isColliding()) {
      return UpdateResult.REMOVE;
    }
    double height = EntityUtil.distanceAboveGround(user.entity(), 3.5);
    double smoothedHeight = smoother.add(height);
    if (user.locBlock().isLiquid()) {
      height = 0.5;
    } else if (smoothedHeight > targetHeight) {
      return UpdateResult.REMOVE;
    }
    Block check = user.location().subtract(new Vector3d(0, height + 0.05, 0)).toBlock(user.world());
    if (!predicate.test(check)) {
      return UpdateResult.REMOVE;
    }
    double delta = getPrediction() - height;
    double force = Math.max(-0.5, Math.min(0.5, 0.3 * delta));
    Vector3d velocity = user.direction().setY(0).normalize().multiply(speed).setY(force);
    affect(velocity);
    user.entity().setFallDistance(0);

    render(check.getBlockData());
    postRender();
    return UpdateResult.CONTINUE;
  }

  private boolean isColliding() {
    double playerSpeed = user.velocity().setY(0).length();
    Vector3d direction = user.direction().setY(0).normalize(Vector3d.ZERO);
    Vector3d front = user.eyeLocation().subtract(new Vector3d(0, 0.5, 0))
      .add(direction.multiply(Math.max(speed, playerSpeed)));
    Block block = front.toBlock(user.world());
    return !MaterialUtil.isTransparentOrWater(block) || !block.isPassable();
  }

  private double getPrediction() {
    double playerSpeed = user.velocity().setY(0).length();
    Vector3d offset = user.direction().setY(0).normalize().multiply(Math.max(speed, playerSpeed) * 3);
    AABB userBounds = AABBUtil.entityBounds(user.entity()).at(offset);
    if (!WorldUtil.nearbyBlocks(user.world(), userBounds, block -> true, 1).isEmpty()) {
      return targetHeight - 1;
    }
    return Math.max(1.25, targetHeight - 2);
  }

  protected abstract void render(@NonNull BlockData data);

  protected abstract void postRender();

  protected abstract void affect(@NonNull Vector3d velocity);

  private static final class HeightSmoother {
    private static final int LENGTH = 10;
    private final double[] values = new double[LENGTH];
    private double sum = 0;
    private int index = 0;

    private double add(double value) {
      double prev = values[index];
      values[index] = value;
      index = (index + 1) % LENGTH;
      sum += value - prev;
      return sum / LENGTH;
    }
  }
}
