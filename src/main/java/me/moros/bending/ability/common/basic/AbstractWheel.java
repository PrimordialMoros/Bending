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

import java.util.Collection;

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.raytrace.RayTrace;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.collision.CollisionUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractWheel implements Updatable, SimpleAbility {
  private final User user;

  private final Vector3d dir;
  private final AABB box;
  protected final Disk collider;
  protected final Ray ray;
  protected Vector3d location;

  protected final double radius;

  protected AbstractWheel(@NonNull User user, @NonNull Ray ray, double radius, double speed) {
    this.user = user;
    this.ray = ray;
    this.location = ray.origin;
    this.radius = radius;
    this.dir = ray.direction.normalize().multiply(speed);
    box = new AABB(new Vector3d(-radius, -radius, -radius), new Vector3d(radius, radius, radius));
    AABB bounds = new AABB(new Vector3d(-0.15, -radius, -radius), new Vector3d(0.15, radius, radius));
    double angle = Math.toRadians(user.yaw());
    OBB obb = new OBB(bounds, Vector3d.PLUS_J, angle);
    collider = new Disk(obb, new Sphere(radius));
  }

  @Override
  public @NonNull UpdateResult update() {
    location = location.add(dir);
    if (!user.canBuild(location.toBlock(user.world()))) {
      return UpdateResult.REMOVE;
    }
    if (!resolveMovement()) {
      return UpdateResult.REMOVE;
    }
    Block base = location.subtract(new Vector3d(0, radius + 0.25, 0)).toBlock(user.world());
    if (base.isLiquid()) {
      return UpdateResult.REMOVE;
    }
    render();
    postRender();
    onBlockHit(base.getRelative(BlockFace.UP));
    boolean hit = CollisionUtil.handle(user, collider.at(location), this::onEntityHit);
    return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public boolean onBlockHit(@NonNull Block block) {
    return true;
  }

  @Override
  public @NonNull Collider collider() {
    return collider.at(location);
  }

  public @NonNull Vector3d location() {
    return location;
  }

  // Try to resolve wheel location by checking collider-block intersections.
  public boolean resolveMovement() {
    double r = radius + 0.05;
    Collection<Block> nearbyBlocks = WorldUtil.nearbyBlocks(user.world(), box.at(location));
    Collider checkCollider = collider();
    // Calculate top and bottom positions and add a small buffer
    double topY = location.y() + r;
    double bottomY = location.y() - r;
    for (Block block : nearbyBlocks) {
      AABB blockBounds = AABBUtil.blockBounds(block);
      if (blockBounds.intersects(checkCollider)) {
        if (blockBounds.min.y() > topY) { // Collision on the top part
          return false;
        }
        double resolution = blockBounds.max.y() - bottomY;
        if (Math.abs(resolution) > radius + 0.1) {
          return false;
        } else {
          location = location.add(new Vector3d(0, resolution, 0));
          return checkCollisions(nearbyBlocks);
        }
      }
    }
    // Try to fall if the block below doesn't have a bounding box.
    Vector3d offset = new Vector3d(0, radius - 0.125, 0);
    Vector3d bottom = location.subtract(offset);
    if (bottom.toBlock(user.world()).isPassable()) {
      Vector3d pos = RayTrace.of(bottom, Vector3d.MINUS_J).range(0.75 * radius).blocks(user.world()).position().add(offset);
      Disk tempCollider = collider.at(pos);
      if (nearbyBlocks.stream().map(AABBUtil::blockBounds).noneMatch(tempCollider::intersects)) {
        location = pos;
        return true;
      }
    }
    return checkCollisions(nearbyBlocks);
  }

  private boolean checkCollisions(Collection<Block> nearbyBlocks) {
    // Check if there's any final collisions after all movements.
    Collider checkCollider = collider();
    return nearbyBlocks.stream().map(AABBUtil::blockBounds).noneMatch(checkCollider::intersects);
  }
}

