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

package me.moros.bending.api.collision.geometry;

import me.moros.math.Position;
import me.moros.math.Vector3d;

/**
 * Simple sphere collider.
 */
public sealed interface Sphere extends Collider permits SphereImpl {
  double radius();

  @Override
  default Type type() {
    return Type.SPHERE;
  }

  @Override
  default AABB outer() {
    Vector3d half = halfExtents();
    return AABB.of(position().subtract(half), position().add(half));
  }

  @Override
  default Sphere at(Position point) {
    return of(point.toVector3d(), radius());
  }

  @Override
  default Vector3d halfExtents() {
    return Vector3d.of(radius(), radius(), radius());
  }

  @Override
  default boolean contains(Vector3d point) {
    double distSq = position().distanceSq(point);
    return distSq <= radius() * radius();
  }

  static Sphere of(double radius) {
    return of(Vector3d.ZERO, radius);
  }

  static Sphere of(Vector3d center, double radius) {
    return new SphereImpl(center, radius);
  }
}
