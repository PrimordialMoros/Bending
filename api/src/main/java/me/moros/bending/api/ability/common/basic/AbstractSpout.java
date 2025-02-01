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

package me.moros.bending.api.ability.common.basic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.api.ability.SimpleAbility;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractSpout extends AbstractFlight implements Updatable, SimpleAbility {
  protected final Set<Block> ignore = new HashSet<>();

  protected Predicate<Block> validBlock = x -> true;
  protected AABB collider;

  private final double height;

  protected double distance;

  protected AbstractSpout(User user, double height) {
    super(user);
    this.height = height;
  }

  @Override
  public UpdateResult update() {
    resetSprintAndFall();
    double maxHeight = height + 2; // Buffer for safety
    Block block = blockCast(user.block(), maxHeight, ignore::contains);
    if (block == null || !validBlock.test(block)) {
      return UpdateResult.REMOVE;
    }
    // Remove if player gets too far away from ground.
    distance = user.location().y() - block.y();
    if (distance > maxHeight) {
      return UpdateResult.REMOVE;
    }
    flight.flying(distance <= height);
    // Create a bounding box for collision that extends through the spout from the ground to the player.
    Position pos = user.location().floor();
    collider = AABB.of(Vector3d.of(-0.5, -distance, -0.5), Vector3d.of(0.5, 0, 0.5)).at(pos);
    render();
    postRender();
    return UpdateResult.CONTINUE;
  }

  @Override
  public boolean onEntityHit(Entity entity) {
    return true;
  }

  @Override
  public boolean onBlockHit(Block block) {
    return true;
  }

  @Override
  public Collider collider() {
    return collider;
  }

  public void onDestroy() {
    cleanup();
  }

  public void limitVelocity(Vector3d velocity, double speed) {
    if (velocity.lengthSq() > speed * speed) {
      user.velocity(velocity.normalize().multiply(speed));
    }
  }

  public static @Nullable Block blockCast(Block origin, double distance) {
    return blockCast(origin, distance, b -> false);
  }

  public static @Nullable Block blockCast(Block origin, double distance, Predicate<Block> ignore) {
    for (int i = 0; i < distance; i++) {
      Block check = origin.offset(Direction.DOWN, i);
      boolean isLiquid = check.type().isLiquid() || MaterialUtil.isWaterPlant(check);
      if ((!check.type().isCollidable() && !isLiquid) || ignore.test(check)) {
        continue;
      }
      return check;
    }
    return null;
  }
}

