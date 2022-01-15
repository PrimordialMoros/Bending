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

import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3d;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Simple sphere collider
 */
public class Sphere implements Collider {
  public final Vector3d center;
  public final double radius;

  public Sphere(double radius) {
    this(Vector3d.ZERO, radius);
  }

  public Sphere(@NonNull Vector3d center, double radius) {
    this.center = center;
    this.radius = radius;
  }

  @Override
  public boolean intersects(@NonNull Collider collider) {
    if (collider instanceof DummyCollider) {
      return false;
    } else if (collider instanceof Sphere sphere) {
      return intersects(sphere);
    } else if (collider instanceof AABB aabb) {
      return intersects(aabb);
    } else if (collider instanceof OBB obb) {
      return intersects(obb);
    } else if (collider instanceof Disk) {
      return collider.intersects(this);
    }
    return false;
  }

  public boolean intersects(@NonNull Ray ray) {
    Vector3d m = ray.origin.subtract(center);
    double b = m.dot(ray.direction);
    return b * b - (m.dot(m) - radius * radius) >= 0;
  }

  private boolean intersects(AABB aabb) {
    Vector3d min = aabb.min;
    Vector3d max = aabb.max;
    // Get the point closest to sphere center on the aabb.
    double x = Math.max(min.getX(), Math.min(center.getX(), max.getX()));
    double y = Math.max(min.getY(), Math.min(center.getY(), max.getY()));
    double z = Math.max(min.getZ(), Math.min(center.getZ(), max.getZ()));
    // Check if that point is inside of the sphere.
    return contains(new Vector3d(x, y, z));
  }

  private boolean intersects(OBB obb) {
    Vector3d v = center.subtract(obb.closestPosition(center));
    return v.dot(v) <= radius * radius;
  }

  private boolean intersects(Sphere other) {
    // Spheres will be colliding if their distance apart is less than the sum of the radii.
    return other.center.distanceSq(center) <= Math.pow(radius + other.radius, 2);
  }

  @Override
  public @NonNull Vector3d position() {
    return center;
  }

  @Override
  public @NonNull Sphere at(@NonNull Vector3d point) {
    return new Sphere(point, radius);
  }

  @Override
  public @NonNull Vector3d halfExtents() {
    return new Vector3d(radius, radius, radius);
  }

  public boolean contains(@NonNull Vector3d point) {
    double distSq = center.distanceSq(point);
    return distSq <= radius * radius;
  }
}
