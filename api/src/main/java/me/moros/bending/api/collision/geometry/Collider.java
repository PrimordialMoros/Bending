/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.api.collision.geometry;

import me.moros.math.Position;
import me.moros.math.Vector3d;

/**
 * Represents a collider.
 */
public sealed interface Collider permits AABB, Disk, OBB, Ray, Sphere {
  double EPSILON = 0.01;

  enum Type {SPHERE, AABB, OBB, RAY, DISK}

  /**
   * Get the collider type.
   * @return the collider type
   */
  Type type();

  /**
   * Calculate a tight-fitting AABB that contains this collider (for broad phase collision detection).
   * @return the calculated outer AABB
   */
  AABB outer();

  /**
   * Check if this collider intersects with another.
   * @param other the other collider to check
   * @return true if the two colliders intersect, false otherwise
   */
  default boolean intersects(Collider other) {
    return other != AABB.dummy() && ColliderUtil.intersects(this, other);
  }

  /**
   * Get the center position for this collider.
   * @return the center position
   */
  Vector3d position();

  /**
   * Calculate a new collider as if this instance was moved.
   * @param point the new center position
   * @return the new collider
   */
  Collider at(Position point);

  /**
   * Calculate the half extents for this collider.
   * @return this half extents for this collider
   */
  Vector3d halfExtents();

  /**
   * Check if the given point is within the space of this collider.
   * @param point the point to check
   * @return true if point is contained in this collider
   */
  boolean contains(Vector3d point);
}
