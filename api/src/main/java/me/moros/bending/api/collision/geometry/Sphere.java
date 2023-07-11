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
 * Simple sphere collider.
 */
public class Sphere implements Collider {
  public final Vector3d center;
  public final double radius;

  public Sphere(double radius) {
    this(Vector3d.ZERO, radius);
  }

  public Sphere(Vector3d center, double radius) {
    this.center = center;
    this.radius = radius;
  }

  boolean _intersects(Sphere other) {
    // Spheres will be colliding if their distance apart is less than the sum of the radii.
    double sum = radius + other.radius;
    return other.center.distanceSq(center) <= sum * sum;
  }

  @Override
  public Vector3d position() {
    return center;
  }

  @Override
  public Sphere at(Position point) {
    return new Sphere(point.toVector3d(), radius);
  }

  @Override
  public Vector3d halfExtents() {
    return Vector3d.of(radius, radius, radius);
  }

  @Override
  public boolean contains(Vector3d point) {
    double distSq = center.distanceSq(point);
    return distSq <= radius * radius;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Sphere other = (Sphere) obj;
    return radius == other.radius && center.equals(other.center);
  }

  @Override
  public int hashCode() {
    return 31 * center.hashCode() + Double.hashCode(radius);
  }
}
