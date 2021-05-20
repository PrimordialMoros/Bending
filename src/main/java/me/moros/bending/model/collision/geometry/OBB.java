/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.collision.geometry;

import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.lang.Math.abs;

/**
 * Oriented bounding box
 */
public class OBB implements Collider {
  private final Vector3 center;
  private final Vector3[] axes;
  private final Vector3 e; // Half extents in local space.

  private OBB(Vector3 center, Vector3[] axes, Vector3 halfExtents) {
    this.center = center;
    this.axes = new Vector3[3];
    System.arraycopy(axes, 0, this.axes, 0, 3);
    this.e = halfExtents;
  }

  public OBB(@NonNull AABB aabb) {
    this.center = aabb.position();
    this.axes = new Vector3[]{Vector3.PLUS_I, Vector3.PLUS_J, Vector3.PLUS_K};
    this.e = aabb.halfExtents();
  }

  public OBB(@NonNull AABB aabb, @NonNull Rotation rotation) {
    this.center = rotation.applyTo(aabb.position());
    double[][] m = rotation.getMatrix();
    this.axes = new Vector3[3];
    for (int i = 0; i < 3; i++) {
      this.axes[i] = new Vector3(m[i]);
    }
    this.e = aabb.halfExtents();
  }

  public OBB(@NonNull AABB aabb, @NonNull Vector3 axis, double angle) {
    this(aabb, new Rotation(axis, angle));
  }

  @Override
  public boolean intersects(@NonNull Collider collider) {
    if (collider instanceof DummyCollider) {
      return false;
    } else if (collider instanceof Sphere) {
      return collider.intersects(this);
    } else if (collider instanceof AABB) {
      return intersects(new OBB((AABB) collider));
    } else if (collider instanceof OBB) {
      return intersects((OBB) collider);
    } else if (collider instanceof Disk) {
      return collider.intersects(this);
    }
    return false;
  }

  private boolean intersects(OBB other) {
    final Vector3 pos = other.center.subtract(center);
    for (int i = 0; i < 3; i++) {
      if (getSeparatingPlane(pos, axes[i], other) || getSeparatingPlane(pos, other.axes[i], other)) {
        return false;
      }
    }
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (getSeparatingPlane(pos, axes[i].crossProduct(other.axes[j]), other)) {
          return false;
        }
      }
    }
    return true;
  }

  // check if there's a separating plane in between the selected axes
  private boolean getSeparatingPlane(Vector3 pos, Vector3 plane, OBB other) {
    final double dot = abs(pos.dotProduct(plane));
    final double x1 = abs((axes[0].multiply(e.x)).dotProduct(plane));
    final double y1 = abs((axes[1].multiply(e.y)).dotProduct(plane));
    final double z1 = abs((axes[2].multiply(e.z)).dotProduct(plane));
    final double x2 = abs((other.axes[0].multiply(other.e.x)).dotProduct(plane));
    final double y2 = abs((other.axes[1].multiply(other.e.y)).dotProduct(plane));
    final double z2 = abs((other.axes[2].multiply(other.e.z)).dotProduct(plane));
    return dot > x1 + y1 + z1 + x2 + y2 + z2;
  }

  // Returns the position closest to the target that lies on/in the OBB.
  public @NonNull Vector3 closestPosition(@NonNull Vector3 target) {
    Vector3 t = target.subtract(center);
    Vector3 closest = center;
    double[] extentArray = e.toArray();
    for (int i = 0; i < 3; i++) {
      Vector3 axis = axes[i];
      double r = extentArray[i];
      double dist = Math.max(-r, Math.min(t.dotProduct(axis), r));
      closest = closest.add(axis.multiply(dist));
    }
    return closest;
  }

  @Override
  public @NonNull Vector3 position() {
    return center;
  }

  @Override
  public @NonNull OBB at(@NonNull Vector3 point) {
    return new OBB(point, axes, e);
  }

  @Override
  public @NonNull Vector3 halfExtents() {
    double x = e.dotProduct(Vector3.PLUS_I);
    double y = e.dotProduct(Vector3.PLUS_J);
    double z = e.dotProduct(Vector3.PLUS_K);
    return new Vector3(x, y, z);
  }

  @Override
  public boolean contains(@NonNull Vector3 point) {
    return closestPosition(point).distanceSq(point) <= 0.01;
  }
}
