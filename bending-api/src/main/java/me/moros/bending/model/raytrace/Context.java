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

package me.moros.bending.model.raytrace;

import java.util.Objects;
import java.util.function.Predicate;

import me.moros.bending.platform.entity.Entity;
import me.moros.math.Vector3d;

/**
 * Represents the context of a ray trace.
 */
public interface Context {
  Vector3d origin();

  Vector3d endPoint();

  default Vector3d dir() {
    return endPoint().subtract(origin());
  }

  double range();

  double raySize();

  boolean ignoreLiquids();

  boolean ignorePassable();

  boolean ignore(int x, int y, int z);

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
