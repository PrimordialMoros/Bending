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

import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;

/**
 * Ray with origin and direction.
 */
public sealed interface Ray extends Collider permits RayImpl {
  Ray ZERO = of(Vector3d.ZERO, Vector3d.ZERO);

  Vector3d direction();

  Vector3d inv();

  @Override
  default Type type() {
    return Type.RAY;
  }

  @Override
  default AABB outer() {
    return AABB.fromRay(this, 0);
  }

  @Override
  default Ray at(Position point) {
    return of(point.toVector3d(), direction());
  }

  @Override
  default Vector3d halfExtents() {
    return direction().multiply(0.5);
  }

  @Override
  default boolean contains(Vector3d point) {
    double lengthSq = direction().lengthSq();
    if (lengthSq == 0) {
      return position().distanceSq(point) <= EPSILON;
    }
    double t = FastMath.clamp(point.subtract(position()).dot(direction()) / lengthSq, 0, 1);
    return position().add(direction().multiply(t)).distanceSq(point) <= EPSILON;
  }

  static Ray of(Vector3d origin, Vector3d direction) {
    return new RayImpl(origin, direction);
  }
}
