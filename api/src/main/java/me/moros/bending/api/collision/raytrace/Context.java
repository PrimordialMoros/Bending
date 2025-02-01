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

package me.moros.bending.api.collision.raytrace;

import java.util.Objects;
import java.util.function.Predicate;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;

/**
 * Represents the context of a ray trace.
 */
public interface Context {
  /**
   * Get the origin of the raytrace.
   * @return the origin vector
   */
  Vector3d origin();

  /**
   * Get the endpoint of the raytrace.
   * @return the endpoint vector
   */
  Vector3d endPoint();

  /**
   * Get the direction of the raytrace.
   * @return the direction vector
   */
  default Vector3d dir() {
    return endPoint().subtract(origin());
  }

  /**
   * Get the range of the raytrace.
   * @return the range
   */
  double range();

  /**
   * Get the size of the raytrace.
   * @return the ray size
   */
  double raySize();

  /**
   * Check if the raytrace should ignore liquids.
   * @return true if the raytrace should ignore liquids, false otherwise
   */
  boolean ignoreLiquids();

  /**
   * Check if the raytrace should ignore passable blocks.
   * @return true if the raytrace should ignore passable blocks, false otherwise
   */
  boolean ignorePassable();

  /**
   * Check if the specified position should be ignored.
   * @param x the x block coordinate
   * @param y the y block coordinate
   * @param z the z block coordinate
   * @return true if the specified position should be ignored, false otherwise
   */
  boolean ignore(int x, int y, int z);

  /**
   * Get the entity filtering predicate for this raytrace context.
   * @return the entity predicate
   */
  Predicate<Entity> entityPredicate();

  /**
   * Create a new builder instance using the specified origin and direction.
   * <p>Note: The range is calculated based on the length of the direction vector.
   * @param origin the origin of the raytrace
   * @param direction the direction of the raytrace
   * @return a new builder instance
   */
  static ContextBuilder builder(Vector3d origin, Vector3d direction) {
    Objects.requireNonNull(origin);
    Objects.requireNonNull(direction);
    return new ContextBuilder(origin, direction);
  }
}
