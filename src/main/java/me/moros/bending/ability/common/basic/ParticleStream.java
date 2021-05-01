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

import java.util.function.Predicate;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;

public abstract class ParticleStream implements Updatable, SimpleAbility {
  private final User user;
  protected final Ray ray;

  protected Predicate<Block> canCollide = b -> false;
  protected Sphere collider;
  protected Vector3 location;
  protected final Vector3 dir;

  protected boolean livingOnly = true;
  protected boolean singleCollision = false;
  protected boolean controllable = false;
  protected int steps = 1;

  protected final double speed;
  protected final double maxRange;
  protected final double collisionRadius;

  public ParticleStream(@NonNull User user, @NonNull Ray ray, double speed, double collisionRadius) {
    this.user = user;
    this.ray = ray;
    this.speed = speed;
    this.location = ray.origin;
    this.maxRange = ray.direction.getNormSq();
    this.collisionRadius = collisionRadius;
    this.collider = new Sphere(location, collisionRadius);
    dir = ray.direction.normalize().scalarMultiply(speed);
    render();
  }

  @Override
  public @NonNull UpdateResult update() {
    Vector3 vector = controllable ? user.direction().scalarMultiply(speed) : dir;
    for (int i = 0; i < steps; i++) {
      Vector3 originalVector = new Vector3(location.toArray());
      location = location.add(vector);
      if (location.distanceSq(ray.origin) > maxRange || !Bending.game().protectionSystem().canBuild(user, location.toBlock(user.world()))) {
        return UpdateResult.REMOVE;
      }
      render();
      postRender();

      if (i % NumberConversions.ceil(speed * steps) != 0) {
        continue; // Avoid unnecessary collision checks
      }
      // Use previous collider for entity checks for visual reasons
      boolean hitEntity = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, false, singleCollision);
      if (hitEntity) {
        return UpdateResult.REMOVE;
      }
      collider = collider.at(location);

      Block originBlock = originalVector.toBlock(user.world());
      for (Vector3 v : VectorMethods.decomposeDiagonals(originalVector, vector)) {
        int x = NumberConversions.floor(v.getX());
        int y = NumberConversions.floor(v.getY());
        int z = NumberConversions.floor(v.getZ());
        Block block = originBlock.getRelative(x, y, z);
        if (canCollide.test(block) && onBlockHit(block)) {
          return UpdateResult.REMOVE;
        }
        if (!MaterialUtil.isTransparent(block)) {
          if (AABBUtils.blockBounds(block).intersects(collider)) {
            if (onBlockHit(block)) {
              return UpdateResult.REMOVE;
            }
          }
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  public @NonNull Location bukkitLocation() {
    return location.toLocation(user.world());
  }

  @Override
  public @NonNull Collider collider() {
    return collider;
  }
}
