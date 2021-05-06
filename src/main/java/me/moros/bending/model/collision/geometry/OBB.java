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
import me.moros.bending.model.math.Vector3;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.NonNull;

// Oriented bounding box
public class OBB implements Collider {
  private static final double epsilon = 0.001;
  private final Vector3 center;
  private final RealMatrix basis;
  private final Vector3 e; // Half extents in local space.

  private OBB(Vector3 center, RealMatrix basis, Vector3 halfExtents) {
    this.center = center;
    this.basis = basis;
    this.e = halfExtents;
  }

  private OBB(AABB aabb) {
    this.center = aabb.position();
    this.basis = MatrixUtils.createRealIdentityMatrix(3);
    this.e = aabb.halfExtents();
  }

  public OBB(@NonNull AABB aabb, @NonNull Rotation rotation) {
    double[] arr = new double[3];
    rotation.applyTo(aabb.position().toArray(), arr);
    this.center = new Vector3(arr);
    this.basis = MatrixUtils.createRealMatrix(rotation.getMatrix());
    this.e = aabb.halfExtents();
  }

  public OBB(@NonNull AABB aabb, @NonNull Vector3 axis, double angle) {
    this(aabb, new Rotation(axis, angle, RotationConvention.VECTOR_OPERATOR));
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
    double ra, rb;
    RealMatrix R = getRotationMatrix(other);
    // translation
    Vector3 t = other.center.subtract(center);
    // Bring into coordinate frame
    t = new Vector3(basis.operate(t.toArray()));
    RealMatrix absR = MatrixUtils.createRealMatrix(3, 3);

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        absR.setEntry(i, j, FastMath.abs(R.getEntry(i, j)) + epsilon);
      }
    }

    // test this box's axes
    for (int i = 0; i < 3; ++i) {
      Vector3 row = new Vector3(absR.getRow(i));
      ra = e.component(i);
      rb = other.e.dotProduct(row);
      if (FastMath.abs(t.component(i)) > ra + rb) {
        return false;
      }
    }

    // test other box's axes
    for (int i = 0; i < 3; ++i) {
      Vector3 col = new Vector3(absR.getColumn(i));
      ra = e.dotProduct(col);
      rb = other.e.component(i);
      Vector3 rotCol = new Vector3(R.getColumn(i));
      if (FastMath.abs(t.dotProduct(rotCol)) > ra + rb) {
        return false;
      }
    }

    // A0 x B0
    ra = e.component(1) * absR.getEntry(2, 0) + e.component(2) * absR.getEntry(1, 0);
    rb = other.e.component(1) * absR.getEntry(0, 2) + other.e.component(2) * absR.getEntry(0, 1);
    if (FastMath.abs(t.component(2) * R.getEntry(1, 0) - t.component(1) * R.getEntry(2, 0)) > ra + rb) {
      return false;
    }

    // A0 x B1
    ra = e.component(1) * absR.getEntry(2, 1) + e.component(2) * absR.getEntry(1, 1);
    rb = other.e.component(0) * absR.getEntry(0, 2) + other.e.component(2) * absR.getEntry(0, 0);
    if (FastMath.abs(t.component(2) * R.getEntry(1, 1) - t.component(1) * R.getEntry(2, 1)) > ra + rb) {
      return false;
    }

    // A0 x B2
    ra = e.component(1) * absR.getEntry(2, 2) + e.component(2) * absR.getEntry(1, 2);
    rb = other.e.component(0) * absR.getEntry(0, 1) + other.e.component(1) * absR.getEntry(0, 0);
    if (FastMath.abs(t.component(2) * R.getEntry(1, 2) - t.component(1) * R.getEntry(2, 2)) > ra + rb) {
      return false;
    }

    // A1 x B0
    ra = e.component(0) * absR.getEntry(2, 0) + e.component(2) * absR.getEntry(0, 0);
    rb = other.e.component(1) * absR.getEntry(1, 2) + other.e.component(2) * absR.getEntry(1, 1);
    if (FastMath.abs(t.component(0) * R.getEntry(2, 0) - t.component(2) * R.getEntry(0, 0)) > ra + rb) {
      return false;
    }

    // A1 x B1
    ra = e.component(0) * absR.getEntry(2, 1) + e.component(2) * absR.getEntry(0, 1);
    rb = other.e.component(0) * absR.getEntry(1, 2) + other.e.component(2) * absR.getEntry(1, 0);
    if (FastMath.abs(t.component(0) * R.getEntry(2, 1) - t.component(2) * R.getEntry(0, 1)) > ra + rb) {
      return false;
    }

    // A1 x B2
    ra = e.component(0) * absR.getEntry(2, 2) + e.component(2) * absR.getEntry(0, 2);
    rb = other.e.component(0) * absR.getEntry(1, 1) + other.e.component(1) * absR.getEntry(1, 0);
    if (FastMath.abs(t.component(0) * R.getEntry(2, 2) - t.component(2) * R.getEntry(0, 2)) > ra + rb) {
      return false;
    }

    // A2 x B0
    ra = e.component(0) * absR.getEntry(1, 0) + e.component(1) * absR.getEntry(0, 0);
    rb = other.e.component(1) * absR.getEntry(2, 2) + other.e.component(2) * absR.getEntry(2, 1);
    if (FastMath.abs(t.component(1) * R.getEntry(0, 0) - t.component(0) * R.getEntry(1, 0)) > ra + rb) {
      return false;
    }

    // A2 x B1
    ra = e.component(0) * absR.getEntry(1, 1) + e.component(1) * absR.getEntry(0, 1);
    rb = other.e.component(0) * absR.getEntry(2, 2) + other.e.component(2) * absR.getEntry(2, 0);
    if (FastMath.abs(t.component(1) * R.getEntry(0, 1) - t.component(0) * R.getEntry(1, 1)) > ra + rb) {
      return false;
    }

    // A2 x B2
    ra = e.component(0) * absR.getEntry(1, 2) + e.component(1) * absR.getEntry(0, 2);
    rb = other.e.component(0) * absR.getEntry(2, 1) + other.e.component(1) * absR.getEntry(2, 0);
    return !(FastMath.abs(t.component(1) * R.getEntry(0, 2) - t.component(0) * R.getEntry(1, 2)) > ra + rb);
  }

  // Express the other box's basis in this box's coordinate frame.
  private RealMatrix getRotationMatrix(OBB other) {
    RealMatrix r = MatrixUtils.createRealMatrix(3, 3);
    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        Vector3 a = new Vector3(basis.getRow(i));
        Vector3 b = new Vector3(other.basis.getRow(j));
        r.setEntry(i, j, a.dotProduct(b));
      }
    }
    return r;
  }

  // Returns the position closest to the target that lies on/in the OBB.
  public @NonNull Vector3 closestPosition(@NonNull Vector3 target) {
    Vector3 t = target.subtract(center);
    Vector3 closest = center;
    // Project target onto basis axes and move toward it.
    for (int i = 0; i < 3; ++i) {
      Vector3 axis = new Vector3(basis.getRow(i));
      double r = e.component(i);
      double dist = FastMath.max(-r, FastMath.min(t.dotProduct(axis), r));
      closest = closest.add(axis.scalarMultiply(dist));
    }
    return closest;
  }

  @Override
  public @NonNull Vector3 position() {
    return center;
  }

  @Override
  public @NonNull OBB at(@NonNull Vector3 point) {
    return new OBB(point, basis, e);
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
    return closestPosition(point).distanceSq(point) <= epsilon;
  }
}
