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
import me.moros.math.Rotation;
import me.moros.math.Vector3d;

/**
 * Oriented bounding box.
 */
public sealed interface OBB extends Collider permits OBBImpl {
  Vector3d axis(int idx);

  Vector3d extents();

  Vector3d localSpace(Vector3d v);

  /**
   * Calculate the position closest to the target that lies on/in the OBB.
   * @param target the target point to calculate relative to
   * @return the result
   */
  Vector3d closestPosition(Vector3d target);

  @Override
  default Type type() {
    return Type.OBB;
  }

  @Override
  OBB at(Position point);

  @Override
  default Vector3d halfExtents() {
    return localSpace(extents()).abs();
  }

  @Override
  default boolean contains(Vector3d point) {
    return closestPosition(point).distanceSq(point) <= EPSILON;
  }

  static OBB of(AABB aabb, Vector3d axis, double angle) {
    return of(aabb, Rotation.from(axis, angle));
  }

  static OBB of(AABB aabb, Rotation rotation) {
    return OBBImpl.from(aabb, rotation);
  }

  static OBB of(AABB aabb) {
    return new OBBImpl(aabb.position(), aabb.halfExtents(), aabb, new Vector3d[]{Vector3d.PLUS_I, Vector3d.PLUS_J, Vector3d.PLUS_K});
  }
}
