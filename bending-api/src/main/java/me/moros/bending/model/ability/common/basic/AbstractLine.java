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

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.raytrace.EntityRayTrace;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
    this.location = Vector3d.from(source.getLocation().add(0.5, 1.25, 0.5));
    this.origin = location;
    this.range = range;
    this.speed = speed;
    EntityRayTrace result = user.rayTrace(range).entities(user.world());
    target = result.entity();
    targetLocation = result.entityCenterOrPosition();
    locked = followTarget && target != null;
    direction = targetLocation.subtract(location).withY(0).normalize();
  }

  @Override
  public UpdateResult update() {
    if (locked) {
      if (isValidTarget()) {
        targetLocation = Vector3d.from(target.getLocation());
        direction = targetLocation.subtract(location).withY(0).normalize();
      } else {
        locked = false;
      }
    }

    if (controllable) {
      targetLocation = user.rayTrace(range).entities(user.world()).entityCenterOrPosition();
      direction = targetLocation.subtract(origin).withY(0).normalize();
    }

    if (onBlockHit(location.toBlock(user.world()).getRelative(BlockFace.DOWN))) {
      return UpdateResult.REMOVE;
    }

    collider = new Sphere(location, 1);
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

    Block block = location.toBlock(user.world());

    if (skipVertical) { // Advance location vertically if possible to match target height
      int y1 = FastMath.floor(targetLocation.y());
      int y2 = FastMath.floor(resolved.point().y());
      if (y1 > y2 && isValidBlock(block.getRelative(BlockFace.UP))) {
        location = resolved.point().add(Vector3d.PLUS_J);
        block = block.getRelative(BlockFace.UP);
      } else if (y1 < y2 && isValidBlock(block.getRelative(BlockFace.DOWN))) {
        location = resolved.point().add(Vector3d.MINUS_J);
        block = block.getRelative(BlockFace.DOWN);
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
    if (target == null || !target.isValid()) {
      return false;
    }
    if (target instanceof Player player && !player.isOnline()) {
      return false;
    }
    return target.getWorld().equals(user.world()) && targetLocation.distanceSq(Vector3d.from(target.getLocation())) < 5 * 5;
  }
}
