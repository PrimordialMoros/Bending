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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.user.User;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;

public abstract class ParticleStream implements Updatable, SimpleAbility {
  private final User user;
  protected final Ray ray;

  protected Predicate<Block> canCollide = b -> false;
  protected Sphere collider;
  protected Vector3d location;
  protected final Vector3d dir;

  protected boolean livingOnly = true;
  protected boolean singleCollision = false;
  protected int steps = 1;
  protected double distanceTravelled = 0;

  protected final double speed;
  protected final double maxRange;
  protected final double collisionRadius;

  protected ParticleStream(User user, Ray ray, double speed, double collisionRadius) {
    this.user = user;
    this.ray = ray;
    this.speed = speed;
    this.location = ray.origin;
    this.maxRange = ray.direction.length();
    this.collisionRadius = collisionRadius;
    this.collider = new Sphere(location, collisionRadius);
    dir = ray.direction.normalize().multiply(speed);
  }

  @Override
  public UpdateResult update() {
    Vector3d vector = controlDirection();
    int d = FastMath.ceil(speed * steps);
    for (int i = 0; i < steps; i++) {
      render();
      postRender();
      if (steps <= 1 || i % d == 0) {
        boolean hitEntity = CollisionUtil.handle(user, collider, this::onEntityHit, livingOnly, false, singleCollision);
        if (hitEntity) {
          return UpdateResult.REMOVE;
        }
      }

      Vector3d originalVector = new Vector3d(location.toArray());
      location = location.add(vector);
      distanceTravelled += speed;
      if (location.distanceSq(ray.origin) > maxRange * maxRange || !user.canBuild(location.toBlock(user.world()))) {
        return UpdateResult.REMOVE;
      }
      if (!validDiagonals(originalVector, vector)) {
        return UpdateResult.REMOVE;
      }
      collider = collider.at(location);
    }
    return UpdateResult.CONTINUE;
  }

  private boolean validDiagonals(Vector3d originalVector, Vector3d directionVector) {
    Block originBlock = originalVector.toBlock(user.world());
    Set<Block> toCheck = new HashSet<>();
    if (speed > 1) {
      toCheck.add(originalVector.add(directionVector.multiply(0.5)).toBlock(user.world()));
    }
    for (Vector3i v : VectorUtil.decomposeDiagonals(originalVector, directionVector)) {
      toCheck.add(originBlock.getRelative(v.x(), v.y(), v.z()));
    }
    return toCheck.stream().noneMatch(this::testCollision);
  }

  private boolean testCollision(Block block) {
    if (canCollide.test(block) && onBlockHit(block)) {
      return true;
    }
    if (!MaterialUtil.isTransparent(block)) {
      if (AABBUtil.blockBounds(block).intersects(collider)) {
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
