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

package me.moros.bending.api.collision.raytrace;

import java.util.Objects;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;

/**
 * Represents a raytrace result.
 */
public interface RayTrace {
  /**
   * The result position of the raytrace.
   * If {@link #hit()} is false then returns the end point of the raytrace.
   * @return the hit position
   */
  Vector3d position();

  /**
   * Check if the raytrace was successful.
   * @return whether the raytrace hit anything
   */
  boolean hit();

  /**
   * Create a raytrace result pointing at the provided position without hitting anything.
   * @param position the vector to point to
   * @return the raytrace result
   */
  static CompositeRayTrace miss(Vector3d position) {
    Objects.requireNonNull(position);
    return new RayTraceImpl(position, null, null);
  }

  /**
   * Create a raytrace result hitting the specified block at the specified position.
   * @param position the vector to point to
   * @param block the block to hit
   * @return the raytrace result
   */
  static CompositeRayTrace hit(Vector3d position, Block block) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(block);
    return new RayTraceImpl(position, block, null);
  }

  /**
   * Create a raytrace result hitting the specified entity at the specified position.
   * @param position the vector to point to
   * @param entity the entity to hit
   * @return the raytrace result
   */
  static CompositeRayTrace hit(Vector3d position, Entity entity) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(entity);
    return new RayTraceImpl(position, null, entity);
  }
}
