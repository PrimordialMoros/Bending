/*
 * Copyright 2020-2024 Moros
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

import me.moros.bending.api.ability.SimpleAbility;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.collision.raytrace.EntityRayTrace;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.user.User;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;

public abstract class AbstractLine extends MovementResolver implements Updatable, SimpleAbility {
  private final User user;

  protected final Vector3d origin;

  protected Vector3d location;
  protected Vector3d targetLocation;
  protected Vector3d direction;
  protected Collider collider;
  protected Entity target;

  protected final double range;
  protected final double speed;

  protected boolean locked;
  protected boolean controllable = false;
  protected boolean skipVertical = false;

  protected AbstractLine(User user, Block source, double range, double speed, boolean followTarget) {
    super(user.world());
    this.user = user;
    this.location = source.toVector3d().add(0.5, 1.25, 0.5);
    this.origin = location;
    this.range = range;
    this.speed = speed;
    EntityRayTrace result = user.rayTrace(range).cast(user.world());
    target = result.entity();
    targetLocation = result.entityCenterOrPosition();
    locked = followTarget && target != null;
    direction = targetLocation.subtract(location).withY(0).normalize();
  }

  @Override
  public UpdateResult update() {
    if (locked) {
      if (isValidTarget()) {
        targetLocation = target.location();
        direction = targetLocation.subtract(location).withY(0).normalize();
      } else {
        locked = false;
      }
    }

    if (controllable) {
      targetLocation = user.rayTrace(range).cast(user.world()).entityCenterOrPosition();
      direction = targetLocation.subtract(origin).withY(0).normalize();
    }

    if (onBlockHit(user.world().blockAt(location).offset(Direction.DOWN))) {
      return UpdateResult.REMOVE;
    }

    collider = Sphere.of(location, 1);
    if (CollisionUtil.handle(user, collider, this, true)) {
      return UpdateResult.REMOVE;
    }

    Resolved resolved = resolve(location, direction);
    if (!resolved.success()) {
      return UpdateResult.REMOVE;
    }
    render();
    location = location.add(resolved.point()).multiply(0.5);
    render(); // Render again at midpoint for a smoother line
    location = resolved.point();
    postRender();

    Block block = user.world().blockAt(location);

    if (skipVertical) { // Advance location vertically if possible to match target height
      int y1 = FastMath.floor(targetLocation.y());
      int y2 = FastMath.floor(resolved.point().y());
      if (y1 > y2 && isValidBlock(block.offset(Direction.UP))) {
        location = resolved.point().add(Vector3d.PLUS_J);
        block = block.offset(Direction.UP);
      } else if (y1 < y2 && isValidBlock(block.offset(Direction.DOWN))) {
        location = resolved.point().add(Vector3d.MINUS_J);
        block = block.offset(Direction.DOWN);
      }
    }

    if (location.distanceSq(origin) > range * range) {
      return UpdateResult.REMOVE;
    }
    if (!user.canBuild(block)) {
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public Collider collider() {
    return collider;
  }

  protected boolean isValidTarget() {
    if (target == null || !target.valid()) {
      return false;
    }
    return target.worldKey().equals(user.worldKey()) && targetLocation.distanceSq(target.location()) < 5 * 5;
  }
}
