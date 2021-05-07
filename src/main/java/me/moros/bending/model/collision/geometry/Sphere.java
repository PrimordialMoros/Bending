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
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Simple sphere collider
 */
public class Sphere implements Collider {
  public final Vector3 center;
  public final double radius;

  public Sphere(double radius) {
    this(Vector3.ZERO, radius);
  }

  public Sphere(@NonNull Vector3 center, double radius) {
    this.center = center;
    this.radius = radius;
  }

  @Override
  public boolean intersects(@NonNull Collider collider) {
    if (collider instanceof DummyCollider) {
      return false;
    } else if (collider instanceof Sphere) {
      return intersects((Sphere) collider);
    } else if (collider instanceof AABB) {
      return intersects((AABB) collider);
    } else if (collider instanceof OBB) {
      return intersects((OBB) collider);
    } else if (collider instanceof Disk) {
      return collider.intersects(this);
    }
    return false;
  }

  public boolean intersects(@NonNull Ray ray) {
    Vector3 m = ray.origin.subtract(center);
    double b = m.dotProduct(ray.direction);
    return b * b - (m.dotProduct(m) - radius * radius) >= 0;
  }

  private boolean intersects(AABB aabb) {
    Vector3 min = aabb.min;
    Vector3 max = aabb.max;
    // Get the point closest to sphere center on the aabb.
    double x = Math.max(min.x, Math.min(center.x, max.x));
    double y = Math.max(min.y, Math.min(center.y, max.y));
    double z = Math.max(min.z, Math.min(center.z, max.z));
    // Check if that point is inside of the sphere.
    return contains(new Vector3(x, y, z));
  }

  private boolean intersects(OBB obb) {
    Vector3 v = center.subtract(obb.closestPosition(center));
    return v.dotProduct(v) <= radius * radius;
  }

  private boolean intersects(Sphere other) {
    // Spheres will be colliding if their distance apart is less than the sum of the radii.
    return other.center.distanceSq(center) <= Math.pow(radius + other.radius, 2);
  }

  @Override
  public @NonNull Vector3 position() {
    return center;
  }

  @Override
  public @NonNull Sphere at(@NonNull Vector3 point) {
    return new Sphere(point, radius);
  }

  @Override
  public @NonNull Vector3 halfExtents() {
    return new Vector3(radius, radius, radius);
  }

  public boolean contains(@NonNull Vector3 point) {
    double distSq = center.distanceSq(point);
    return distSq <= radius * radius;
  }
}
