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
 * Axis aligned bounding box.
 */
public sealed interface AABB extends Collider permits AABBImpl, AABBDummy {
  AABB BLOCK_BOUNDS = of(Vector3d.ZERO, Vector3d.ONE);
  AABB EXPANDED_BLOCK_BOUNDS = BLOCK_BOUNDS.grow(Vector3d.of(0.4, 0.4, 0.4));

  /**
   * Get the min
   * @return the min box component
   */
  Vector3d min();

  Vector3d max();

  /**
   * Calculate an AABB by expanding this instance by the given amount in each component.
   * The expansion is uniform and will affect both the min and max points.
   * For example, expanding {@link #BLOCK_BOUNDS} (1x1x1 box) by {@link Vector3d#ONE} will result in a 3x3x3 box.
   * @param diff the amount to expand
   * @return the expanded AABB
   */
  default AABB grow(Vector3d diff) {
    return of(min().subtract(diff), max().add(diff));
  }

  @Override
  default Vector3d position() {
    return min().add(max().subtract(min()).multiply(0.5));
  }

  @Override
  default AABB at(Position point) {
    Vector3d halfExtents = halfExtents();
    Vector3d pos = point.toVector3d();
    return of(pos.subtract(halfExtents), pos.add(halfExtents));
  }

  @Override
  default Vector3d halfExtents() {
    return max().subtract(min()).multiply(0.5).abs();
  }

  @Override
  default boolean contains(Vector3d point) {
    return (point.x() >= min().x() && point.x() <= max().x()) &&
      (point.y() >= min().y() && point.y() <= max().y()) &&
      (point.z() >= min().z() && point.z() <= max().z());
  }

  /**
   * Get a dummy AABB collider.
   * @return a dummy collider
   */
  static AABB dummy() {
    return AABBDummy.INSTANCE;
  }

  static AABB fromRay(Ray ray, double raySize) {
    return fromRay(ray.position(), ray.direction(), raySize);
  }

  static AABB fromRay(Vector3d start, Vector3d dir, double raySize) {
    if (dir.lengthSq() == 0) {
      return dummy();
    }
    double offset = Math.max(0, raySize);
    double newMinX = start.x() - (dir.x() < 0 ? -dir.x() : 0) - offset;
    double newMinY = start.y() - (dir.y() < 0 ? -dir.y() : 0) - offset;
    double newMinZ = start.z() - (dir.z() < 0 ? -dir.z() : 0) - offset;
    double newMaxX = start.x() + (dir.x() > 0 ? dir.x() : 0) + offset;
    double newMaxY = start.y() + (dir.y() > 0 ? dir.y() : 0) + offset;
    double newMaxZ = start.z() + (dir.z() > 0 ? dir.z() : 0) + offset;
    return of(Vector3d.of(newMinX, newMinY, newMinZ), Vector3d.of(newMaxX, newMaxY, newMaxZ));
  }

  static AABB of(Vector3d min, Vector3d max) {
    return new AABBImpl(min, max);
  }
}
