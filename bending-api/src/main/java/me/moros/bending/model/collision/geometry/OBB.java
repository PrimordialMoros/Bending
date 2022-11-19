/*
 * Copyright 2020-2022 Moros
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

import java.util.Arrays;

import me.moros.math.FastMath;
import me.moros.math.Rotation;
import me.moros.math.Vector3d;

import static java.lang.Math.abs;

/**
 * Oriented bounding box.
 */
public class OBB implements Collider {
  public final Vector3d center;
  public final Vector3d e; // Half extents in local space.
  final AABB outer;
  final Vector3d[] axes;

  private OBB(OBB obb, Vector3d center) {
    this.center = center;
    this.e = obb.e;
    this.axes = new Vector3d[3];
    this.outer = obb.outer.at(center);
    System.arraycopy(obb.axes, 0, axes, 0, 3);
  }

  public OBB(AABB aabb) {
    this.center = aabb.position();
    this.e = aabb.halfExtents();
    this.outer = aabb;
    this.axes = new Vector3d[]{Vector3d.PLUS_I, Vector3d.PLUS_J, Vector3d.PLUS_K};
  }

  public OBB(AABB aabb, Rotation rotation) {
    this.center = rotation.applyTo(aabb.position());
    this.e = aabb.halfExtents();
    double[][] m = rotation.getMatrix();
    this.axes = new Vector3d[3];
    for (int i = 0; i < 3; i++) {
      this.axes[i] = Vector3d.from(m[i]);
    }
    Vector3d halfExtents = halfExtents();
    this.outer = new AABB(center.subtract(halfExtents), center.add(halfExtents));
  }

  public OBB(AABB aabb, Vector3d axis, double angle) {
    this(aabb, Rotation.from(axis, angle));
  }

  boolean _intersects(OBB other) {
    if (!outer._intersects(other.outer)) {
      return false;
    }
    final Vector3d pos = other.center.subtract(center);
    for (int i = 0; i < 3; i++) {
      if (getSeparatingPlane(pos, axes[i], other) || getSeparatingPlane(pos, other.axes[i], other)) {
        return false;
      }
    }
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (getSeparatingPlane(pos, axes[i].cross(other.axes[j]), other)) {
          return false;
        }
      }
    }
    return true;
  }

  Vector3d localSpace(Vector3d dir) {
    double[] out = new double[3];
    for (int row = 0; row < 3; row++) {
      out[row] = axes[row].dot(dir);
    }
    return Vector3d.from(out);
  }

  // check if there's a separating plane in between the selected axes
  private boolean getSeparatingPlane(Vector3d pos, Vector3d plane, OBB other) {
    final double dot = abs(pos.dot(plane));
    final double x1 = abs((axes[0].multiply(e.x())).dot(plane));
    final double y1 = abs((axes[1].multiply(e.y())).dot(plane));
    final double z1 = abs((axes[2].multiply(e.z())).dot(plane));
    final double x2 = abs((other.axes[0].multiply(other.e.x())).dot(plane));
    final double y2 = abs((other.axes[1].multiply(other.e.y())).dot(plane));
    final double z2 = abs((other.axes[2].multiply(other.e.z())).dot(plane));
    return dot > x1 + y1 + z1 + x2 + y2 + z2;
  }

  // Returns the position closest to the target that lies on/in the OBB.
  public Vector3d closestPosition(Vector3d target) {
    Vector3d t = target.subtract(center);
    Vector3d closest = center;
    double[] extentArray = e.toArray();
    for (int i = 0; i < 3; i++) {
      Vector3d axis = axes[i];
      double r = extentArray[i];
      double dist = FastMath.clamp(t.dot(axis), -r, r);
      closest = closest.add(axis.multiply(dist));
    }
    return closest;
  }

  @Override
  public Vector3d position() {
    return center;
  }

  @Override
  public OBB at(Vector3d point) {
    return new OBB(this, point);
  }

  @Override
  public Vector3d halfExtents() {
    return localSpace(e).abs();
  }

  @Override
  public boolean contains(Vector3d point) {
    return closestPosition(point).distanceSq(point) <= EPSILON;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    OBB other = (OBB) obj;
    return center.equals(other.center) && e.equals(other.e) && Arrays.equals(axes, other.axes);
  }

  @Override
  public int hashCode() {
    int result = center.hashCode();
    result = 31 * result + e.hashCode();
    result = 31 * result + Arrays.hashCode(axes);
    return result;
  }
}
