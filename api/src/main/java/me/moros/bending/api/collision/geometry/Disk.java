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
 * Combination of {@link Sphere} and {@link OBB} to simulate a disk collider.
 */
public sealed interface Disk extends Collider permits DiskImpl {
  Sphere sphere();

  OBB obb();

  @Override
  default Type type() {
    return Type.DISK;
  }

  @Override
  default AABB outer() {
    AABB box1 = sphere().outer();
    AABB box2 = obb().outer();
    return AABB.of(box1.min().min(box2.min()), box1.max().max(box2.max()));
  }

  @Override
  default Vector3d position() {
    return sphere().position();
  }

  @Override
  default Disk at(Position point) {
    return of(sphere().at(point), obb().at(point));
  }

  @Override
  default Vector3d halfExtents() {
    return obb().halfExtents();
  }

  @Override
  default boolean contains(Vector3d point) {
    return sphere().contains(point) && obb().contains(point);
  }

  static Disk of(Sphere sphere, OBB obb) {
    return new DiskImpl(sphere, obb);
  }
}
