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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    this.maxRange = ray.direction.getNorm();
    this.collisionRadius = collisionRadius;
    this.collider = new Sphere(location, collisionRadius);
    dir = ray.direction.normalize().multiply(speed);
    render();
  }

  @Override
  public @NonNull UpdateResult update() {
    Vector3 vector = controllable ? user.direction().multiply(speed) : dir;
    for (int i = 0; i < steps; i++) {
      Vector3 originalVector = new Vector3(location.toArray());
      location = location.add(vector);
      if (location.distanceSq(ray.origin) > maxRange * maxRange || !Bending.game().protectionSystem().canBuild(user, location.toBlock(user.world()))) {
        return UpdateResult.REMOVE;
      }
      render();
      postRender();

      if (steps > 1 && i % NumberConversions.ceil(speed * steps) != 0) {
        continue; // Avoid unnecessary collision checks
      }
      // Use previous collider for entity checks for visual reasons
      boolean hitEntity = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, false, singleCollision);
      if (hitEntity) {
        return UpdateResult.REMOVE;
      }
      collider = collider.at(location);

      Block originBlock = originalVector.toBlock(user.world());
      Set<Block> toCheck = new HashSet<>();
      if (speed > 1) {
        toCheck.add(originalVector.add(vector.multiply(0.5)).toBlock(user.world()));
      }

      for (IntVector v : VectorMethods.decomposeDiagonals(originalVector, vector)) {
        toCheck.add(originBlock.getRelative(v.x, v.y, v.z));
      }

      if (toCheck.stream().anyMatch(this::testCollision)) {
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  private boolean testCollision(Block block) {
    if (canCollide.test(block) && onBlockHit(block)) {
      return true;
    }
    if (!MaterialUtil.isTransparent(block)) {
      if (AABBUtils.blockBounds(block).intersects(collider)) {
        return onBlockHit(block);
      }
    }
    return false;
  }

  public @NonNull Location bukkitLocation() {
    return location.toLocation(user.world());
  }

  @Override
  public @NonNull Collider collider() {
    return collider;
  }
}
