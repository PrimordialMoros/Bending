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
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import me.moros.math.VectorUtil;

public abstract class ParticleStream implements Updatable, SimpleAbility {
  private final User user;
  protected final Ray ray;

  protected Predicate<BlockType> canCollide = b -> false;
  private Ray collider;
  private Vector3d location;
  protected final Vector3d dir;

  protected boolean livingOnly = true;
  protected boolean selfCollision = false;
  protected boolean singleCollision = false;
  protected int steps = 1;
  protected double distanceTravelled = 0;
  protected double collisionRadius;
  private Vector3d expansion;

  protected final double speed;
  protected final double maxRange;

  protected ParticleStream(User user, Ray ray, double speed, double collisionRadius) {
    this.user = user;
    this.ray = ray;
    this.speed = speed;
    this.location = ray.position();
    this.maxRange = ray.direction().length();
    this.collisionRadius = collisionRadius;
    dir = ray.direction().normalize().multiply(speed);
    this.collider = Ray.of(location, dir.multiply(steps));
    this.expansion = calculateExpansion();
  }

  private Vector3d calculateExpansion() {
    return Vector3d.ONE.multiply(collisionRadius);
  }

  @Override
  public UpdateResult update() {
    Vector3d vector = controlDirection();

    Vector3d originalLocation = location;
    double originalCollisionRadius = collisionRadius;
    expansion = calculateExpansion();

    for (int i = 0; i < steps; i++) {
      render(location);
      postRender(location);
      Vector3d originalVector = location;
      location = location.add(vector);
      distanceTravelled += speed;
      if (location.distanceSq(ray.position()) > maxRange * maxRange || !user.canBuild(location)) {
        return UpdateResult.REMOVE;
      }
      if (!validDiagonals(originalVector, dir)) {
        return UpdateResult.REMOVE;
      }
    }

    collider = Ray.of(originalLocation, vector.multiply(steps));
    AABB outer = AABB.fromRay(collider, originalCollisionRadius);

    boolean hitEntity = CollisionUtil.handle(user, outer, this::onFilteredEntityHit, livingOnly, false, singleCollision);
    if (selfCollision) {
      hitEntity |= onFilteredEntityHit(user);
    }

    return hitEntity ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  protected boolean onFilteredEntityHit(Entity entity) {
    return collider.intersects(entity.bounds().grow(expansion)) && onEntityHit(entity);
  }

  private boolean validDiagonals(Vector3d originalVector, Vector3d directionVector) {
    Block originBlock = user.world().blockAt(originalVector);
    Set<Block> toCheck = new HashSet<>();
    if (speed > 1) {
      toCheck.add(user.world().blockAt(originalVector.add(directionVector.multiply(0.5))));
    }
    for (Vector3i v : VectorUtil.decomposeDiagonals(originalVector, directionVector)) {
      toCheck.add(originBlock.offset(v));
    }
    return toCheck.stream().noneMatch(this::testCollision);
  }

  private boolean testCollision(Block block) {
    if (canCollide.test(block.type()) && onBlockHit(block)) {
      return true;
    }
    if (!MaterialUtil.isTransparent(block)) {
      if (block.bounds().grow(expansion).intersects(collider)) {
        return onBlockHit(block);
      }
    }
    return false;
  }

  protected Vector3d controlDirection() {
    return dir;
  }

  @Override
  public Collider collider() {
    return collider;
  }
}
