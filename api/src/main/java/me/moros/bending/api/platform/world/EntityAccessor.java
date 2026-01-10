/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.platform.world;

import java.util.List;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;

public interface EntityAccessor {
  /**
   * Collects all entities in a sphere.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @return all collected entities
   * @see #nearbyEntities(AABB)
   */
  default List<Entity> nearbyEntities(Vector3d pos, double radius) {
    return nearbyEntities(pos, radius, block -> true, 0);
  }

  /**
   * Collects all entities in a sphere that satisfy the given predicate.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @return all collected entities
   * @see #nearbyEntities(AABB, Predicate)
   */
  default List<Entity> nearbyEntities(Vector3d pos, double radius, Predicate<Entity> predicate) {
    return nearbyEntities(pos, radius, predicate, 0);
  }

  /**
   * Collects all entities in a sphere that satisfy the given predicate.
   * <p>Note: Limit is only respected if positive. Otherwise, all entities that satisfy the given predicate are collected.
   * @param pos the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of entities to collect
   * @return all collected entities
   * @see #nearbyEntities(AABB, Predicate, int)
   */
  default List<Entity> nearbyEntities(Vector3d pos, double radius, Predicate<Entity> predicate, int limit) {
    AABB aabb = AABB.of(pos.subtract(radius, radius, radius), pos.add(radius, radius, radius));
    Predicate<Entity> distPredicate = e -> pos.distanceSq(e.location()) < radius * radius;
    return nearbyEntities(aabb, distPredicate.and(predicate), limit);
  }

  /**
   * Collects all entities inside a bounding box.
   * @param box the bounding box to check
   * @return all collected entities
   * @see #nearbyEntities(Vector3d, double)
   */
  default List<Entity> nearbyEntities(AABB box) {
    return nearbyEntities(box, block -> true, 0);
  }

  /**
   * Collects all entities inside a bounding box that satisfy the given predicate.
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @return all collected entities
   * @see #nearbyEntities(Vector3d, double, Predicate)
   */
  default List<Entity> nearbyEntities(AABB box, Predicate<Entity> predicate) {
    return nearbyEntities(box, predicate, 0);
  }

  /**
   * Collects all entities inside a bounding box that satisfy the given predicate.
   * <p>Note: Limit is only respected if positive. Otherwise, all entities that satisfy the given predicate are collected.
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of entities to collect
   * @return all collected entities
   * @see #nearbyEntities(Vector3d, double, Predicate, int)
   */
  List<Entity> nearbyEntities(AABB box, Predicate<Entity> predicate, int limit);
}
