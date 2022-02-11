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

import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.lang.Math.abs;

/**
 * Oriented bounding box
 */
public class OBB implements Collider {
  public final Vector3d center;
  public final Vector3d e; // Half extents in local space.
  final Vector3d[] axes;

  private OBB(Vector3d center, Vector3d[] axes, Vector3d halfExtents) {
    this.center = center;
    this.e = halfExtents;
    this.axes = new Vector3d[3];
    System.arraycopy(axes, 0, this.axes, 0, 3);
  }

  public OBB(@NonNull AABB aabb) {
    this.center = aabb.position();
    this.e = aabb.halfExtents();
    this.axes = new Vector3d[]{Vector3d.PLUS_I, Vector3d.PLUS_J, Vector3d.PLUS_K};
  }

  public OBB(@NonNull AABB aabb, @NonNull Rotation rotation) {
    this.center = rotation.applyTo(aabb.position());
    this.e = aabb.halfExtents();
    double[][] m = rotation.getMatrix();
    this.axes = new Vector3d[3];
    for (int i = 0; i < 3; i++) {
      this.axes[i] = new Vector3d(m[i]);
    }
  }

  public OBB(@NonNull AABB aabb, @NonNull Vector3d axis, double angle) {
    this(aabb, new Rotation(axis, angle));
  }

  boolean intersects(@NonNull OBB other) {
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
  public @NonNull Vector3d closestPosition(@NonNull Vector3d target) {
    Vector3d t = target.subtract(center);
    Vector3d closest = center;
    double[] extentArray = e.toArray();
    for (int i = 0; i < 3; i++) {
      Vector3d axis = axes[i];
      double r = extentArray[i];
      double dist = Math.max(-r, Math.min(t.dot(axis), r));
      closest = closest.add(axis.multiply(dist));
    }
    return closest;
  }

  @Override
  public @NonNull Vector3d position() {
    return center;
  }

  @Override
  public @NonNull OBB at(@NonNull Vector3d point) {
    return new OBB(point, axes, e);
  }

  @Override
  public @NonNull Vector3d halfExtents() {
    double x = e.dot(Vector3d.PLUS_I);
    double y = e.dot(Vector3d.PLUS_J);
    double z = e.dot(Vector3d.PLUS_K);
    return new Vector3d(x, y, z);
  }

  @Override
  public boolean contains(@NonNull Vector3d point) {
    return closestPosition(point).distanceSq(point) <= 0.01;
  }
}
