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

import java.util.Collection;

import me.moros.bending.api.ability.SimpleAbility;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Disk;
import me.moros.bending.api.collision.geometry.OBB;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.user.User;
import me.moros.math.Vector3d;

public abstract class AbstractWheel implements Updatable, SimpleAbility {
  private final User user;

  private final Vector3d dir;
  private final AABB box;
  protected final Disk collider;
  protected final Ray ray;
  protected Vector3d location;

  protected final double radius;

  protected AbstractWheel(User user, Ray ray, double radius, double speed) {
    this.user = user;
    this.ray = ray;
    this.location = ray.position();
    this.radius = radius;
    this.dir = ray.direction().normalize().multiply(speed);
    box = AABB.of(Vector3d.of(-radius, -radius, -radius), Vector3d.of(radius, radius, radius));
    AABB bounds = AABB.of(Vector3d.of(-0.15, -radius, -radius), Vector3d.of(0.15, radius, radius));
    double angle = Math.toRadians(user.yaw());
    OBB obb = OBB.of(bounds, Vector3d.PLUS_J, angle);
    collider = Disk.of(Sphere.of(radius), obb).at(location);
  }

  @Override
  public UpdateResult update() {
    location = location.add(dir);
    if (!user.canBuild(location)) {
      return UpdateResult.REMOVE;
    }
    if (!resolveMovement()) {
      return UpdateResult.REMOVE;
    }
    Block base = user.world().blockAt(location.subtract(0, radius + 0.25, 0));
    if (base.type().isLiquid()) {
      return UpdateResult.REMOVE;
    }
    render();
    postRender();
    onBlockHit(base.offset(Direction.UP));
    boolean hit = CollisionUtil.handle(user, collider(), this);
    return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public boolean onBlockHit(Block block) {
    return true;
  }

  @Override
  public Collider collider() {
    return collider.at(location);
  }

  public Vector3d location() {
    return location;
  }

  // Try to resolve wheel location by checking collider-block intersections.
  public boolean resolveMovement() {
    double r = radius + 0.05;
    Collection<Block> nearbyBlocks = user.world().nearbyBlocks(box.at(location));
    Collider checkCollider = collider();
    // Calculate top and bottom positions and add a small buffer
    double topY = location.y() + r;
    double bottomY = location.y() - r;
    for (Block block : nearbyBlocks) {
      AABB blockBounds = block.bounds();
      if (blockBounds.intersects(checkCollider)) {
        if (blockBounds.min().y() > topY) { // Collision on the top part
          return false;
        }
        double resolution = blockBounds.max().y() - bottomY;
        if (Math.abs(resolution) > radius + 0.1) {
          return false;
        } else {
          location = location.add(0, resolution, 0);
          return checkCollisions(nearbyBlocks);
        }
      }
    }
    // Try to fall if the block below doesn't have a bounding box.
    Vector3d offset = Vector3d.of(0, radius - 0.125, 0);
    Vector3d bottom = location.subtract(offset);
    if (!user.world().blockAt(bottom).type().isCollidable()) {
      Vector3d pos = Context.builder(bottom, Vector3d.MINUS_J).range(0.75 * radius).blocks(user.world()).position().add(offset);
      Disk tempCollider = collider.at(pos);
      if (nearbyBlocks.stream().map(Block::bounds).noneMatch(tempCollider::intersects)) {
        location = pos;
        return true;
      }
    }
    return checkCollisions(nearbyBlocks);
  }

  private boolean checkCollisions(Collection<Block> nearbyBlocks) {
    // Check if there's any final collisions after all movements.
    Collider checkCollider = collider();
    return nearbyBlocks.stream().map(Block::bounds).noneMatch(checkCollider::intersects);
  }
}

