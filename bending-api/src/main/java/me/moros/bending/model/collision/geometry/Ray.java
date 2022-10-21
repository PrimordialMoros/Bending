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

import me.moros.bending.model.math.Vector3d;

/**
 * Ray with origin and direction.
 */
public class Ray implements Collider {
  public final Vector3d origin;
  public final Vector3d direction;
  public final Vector3d invDir;

  private Ray(Vector3d origin, Vector3d direction, Vector3d invDir) {
    this.origin = origin;
    this.direction = direction;
    this.invDir = invDir;
  }

  public Ray(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction;
    double invX = direction.x() == 0 ? Double.MAX_VALUE : 1 / direction.x();
    double invY = direction.y() == 0 ? Double.MAX_VALUE : 1 / direction.y();
    double invZ = direction.z() == 0 ? Double.MAX_VALUE : 1 / direction.z();
    invDir = new Vector3d(invX, invY, invZ);
  }

  boolean _intersects(Ray other) {
    Vector3d cross = direction.cross(other.direction);
    if (cross.lengthSq() < EPSILON) {
      return contains(other.origin) || other.contains(origin);
    }
    double planarFactor = other.origin.subtract(origin).dot(cross);
    return Math.abs(planarFactor) < EPSILON;
  }

  @Override
  public Vector3d position() {
    return origin;
  }

  @Override
  public Collider at(Vector3d point) {
    return new Ray(point, direction, invDir);
  }

  @Override
  public Vector3d halfExtents() {
    return direction.multiply(0.5);
  }

  @Override
  public boolean contains(Vector3d point) {
    double lengthSq = direction.lengthSq();
    if (lengthSq == 0) {
      return origin.distanceSq(point) <= EPSILON;
    }
    double t = Math.max(0, Math.min(1, point.subtract(origin).dot(direction) / lengthSq));
    return origin.add(direction.multiply(t)).distanceSq(point) <= EPSILON;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Ray other = (Ray) obj;
    return origin.equals(other.origin) && direction.equals(other.direction);
  }

  @Override
  public int hashCode() {
    int result = origin.hashCode();
    result = 31 * result + direction.hashCode();
    return result;
  }
}
