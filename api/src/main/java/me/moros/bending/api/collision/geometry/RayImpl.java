/*
 * Copyright 2020-2024 Moros
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

import java.util.function.Supplier;

import me.moros.bending.api.util.functional.Suppliers;
import me.moros.math.Vector3d;

record RayImpl(Vector3d position, Vector3d direction, Supplier<Vector3d> invSupplier) implements Ray {
  RayImpl(Vector3d position, Vector3d direction) {
    this(position, direction, Suppliers.lazy(() -> calculateInv(direction)));
  }

  @Override
  public Vector3d inv() {
    return invSupplier().get();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RayImpl other = (RayImpl) obj;
    return position.equals(other.position) && direction.equals(other.direction);
  }

  @Override
  public int hashCode() {
    int result = position.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }

  private static Vector3d calculateInv(Vector3d dir) {
    double invX = dir.x() == 0 ? Double.MAX_VALUE : 1 / dir.x();
    double invY = dir.y() == 0 ? Double.MAX_VALUE : 1 / dir.y();
    double invZ = dir.z() == 0 ? Double.MAX_VALUE : 1 / dir.z();
    return Vector3d.of(invX, invY, invZ);
  }
}
