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
public interface Collider {
  double EPSILON = 0.01;

  /**
   * Check if this collider intersects with another.
   * @param other the other collider to check
   * @return true if the two colliders intersect, false otherwise
   */
  default boolean intersects(Collider other) {
    return ColliderUtil.intersects(this, other);
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
