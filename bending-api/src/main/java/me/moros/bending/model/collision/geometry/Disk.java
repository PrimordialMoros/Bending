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

package me.moros.bending.model.collision.geometry;

import me.moros.math.Position;
import me.moros.math.Vector3d;

/**
 * Combination of {@link OBB} and {@link Sphere} to simulate a disk collider.
 */
public record Disk(OBB obb, Sphere sphere) implements Collider {
  @Override
  public Vector3d position() {
    return sphere.center;
  }

  @Override
  public Disk at(Position point) {
    return new Disk(obb.at(point), sphere.at(point));
  }

  @Override
  public Vector3d halfExtents() {
    return obb.halfExtents();
  }

  @Override
  public boolean contains(Vector3d point) {
    return sphere.contains(point) && obb.contains(point);
  }
}
