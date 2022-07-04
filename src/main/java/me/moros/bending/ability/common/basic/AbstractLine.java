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

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.raytrace.RayTraceResult.EntityRayTrace;
import me.moros.bending.util.collision.CollisionUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

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

  protected AbstractLine(@NonNull User user, @NonNull Block source, double range, double speed, boolean followTarget) {
    super(user.world());
    this.user = user;
    this.location = new Vector3d(source.getLocation().add(0.5, 1.25, 0.5));
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
  public @NonNull UpdateResult update() {
    if (locked) {
      if (isValidTarget()) {
        targetLocation = new Vector3d(target.getLocation());
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
    if (CollisionUtil.handle(user, collider, this::onEntityHit, true)) {
      return UpdateResult.REMOVE;
    }

    Vector3d newLocation = resolve(location, direction);
    if (newLocation == null) {
      onCollision();
      return UpdateResult.REMOVE;
    }

    render();
    location = location.add(newLocation).multiply(0.5);
    render(); // Render again at midpoint for a smoother line
    postRender();
    location = newLocation;

    Block block = location.toBlock(user.world());

    if (skipVertical) { // Advance location vertically if possible to match target height
      int y1 = FastMath.floor(targetLocation.y());
      int y2 = FastMath.floor(newLocation.y());
      if (y1 > y2 && isValidBlock(block.getRelative(BlockFace.UP))) {
        location = newLocation.add(Vector3d.PLUS_J);
        block = block.getRelative(BlockFace.UP);
      } else if (y1 < y2 && isValidBlock(block.getRelative(BlockFace.DOWN))) {
        location = newLocation.add(Vector3d.MINUS_J);
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
  public @NonNull Collider collider() {
    return collider;
  }

  protected void onCollision() {
  }

  protected boolean isValidTarget() {
    if (target == null || !target.isValid()) {
      return false;
    }
    if (target instanceof Player player && !player.isOnline()) {
      return false;
    }
    return target.getWorld().equals(user.world()) && targetLocation.distanceSq(new Vector3d(target.getLocation())) < 5 * 5;
  }
}
