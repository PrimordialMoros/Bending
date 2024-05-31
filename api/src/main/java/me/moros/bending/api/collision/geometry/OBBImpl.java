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

import java.util.Arrays;

import me.moros.math.Position;
import me.moros.math.Rotation;
import me.moros.math.Vector3d;

record OBBImpl(Vector3d position, Vector3d extents, AABB outer, Vector3d[] axes) implements OBB {
  @Override
  public Vector3d axis(int idx) {
    return axes[idx];
  }

  @Override
  public Vector3d localSpace(Vector3d v) {
    return localSpace(axes, v);
  }

  @Override
  public Vector3d closestPosition(Vector3d target) {
    Vector3d t = target.subtract(position);
    Vector3d closest = position;
    double[] extentArray = extents.toArray();
    for (int i = 0; i < 3; i++) {
      Vector3d axis = axes[i];
      double r = extentArray[i];
      double dist = Math.clamp(t.dot(axis), -r, r);
      closest = closest.add(axis.multiply(dist));
    }
    return closest;
  }

  @Override
  public OBB at(Position point) {
    var point3d = point.toVector3d();
    return new OBBImpl(point3d, extents, outer.at(point3d), axes.clone());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    OBBImpl other = (OBBImpl) obj;
    return position.equals(other.position) && extents.equals(other.extents) && Arrays.equals(axes, other.axes);
  }

  @Override
  public int hashCode() {
    int result = position.hashCode();
    result = 31 * result + extents.hashCode();
    result = 31 * result + Arrays.hashCode(axes);
    return result;
  }

  private static Vector3d localSpace(Vector3d[] axes, Vector3d dir) {
    return Vector3d.of(axes[0].dot(dir), axes[1].dot(dir), axes[2].dot(dir));
  }

  static OBB from(AABB aabb, Rotation rotation) {
    Vector3d center = rotation.applyTo(aabb.position());
    Vector3d e = aabb.halfExtents();
    double[][] m = rotation.getMatrix();
    Vector3d[] axes = new Vector3d[3];
    for (int i = 0; i < 3; i++) {
      axes[i] = Vector3d.from(m[i]);
    }
    Vector3d halfExtents = localSpace(axes, e).abs();
    return new OBBImpl(center, e, AABB.of(center.subtract(halfExtents), center.add(halfExtents)), axes);
  }
}
