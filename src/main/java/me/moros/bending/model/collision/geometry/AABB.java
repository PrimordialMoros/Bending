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
import me.moros.bending.model.math.Vector3d;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Axis aligned bounding box
 */
public class AABB implements Collider {
  public static final AABB PLAYER_BOUNDS = new AABB(new Vector3d(-0.3, 0.0, -0.3), new Vector3d(0.3, 1.8, 0.3));
  public static final AABB BLOCK_BOUNDS = new AABB(Vector3d.ZERO, Vector3d.ONE);
  public static final AABB EXPANDED_BLOCK_BOUNDS = BLOCK_BOUNDS.grow(new Vector3d(0.4, 0.4, 0.4));

  public final Vector3d min;
  public final Vector3d max;

  public AABB(@NonNull Vector3d min, @NonNull Vector3d max) {
    this.min = min;
    this.max = max;
  }

  public @NonNull AABB grow(@NonNull Vector3d diff) {
    return new AABB(min.subtract(diff), max.add(diff));
  }

  @Override
  public boolean intersects(@NonNull Collider collider) {
    if (collider instanceof DummyCollider) {
      return false;
    } else if (collider instanceof Sphere sphere) {
      return intersects(sphere);
    } else if (collider instanceof AABB aabb) {
      return intersects(aabb);
    } else if (collider instanceof OBB) {
      return collider.intersects(this);
    } else if (collider instanceof Disk) {
      return collider.intersects(this);
    }
    return false;
  }

  private boolean intersects(@NonNull AABB other) {
    return (max.getX() > other.min.getX() && min.getX() < other.max.getX() &&
      max.getY() > other.min.getY() && min.getY() < other.max.getY() &&
      max.getZ() > other.min.getZ() && min.getZ() < other.max.getZ());
  }

  private boolean intersects(@NonNull Sphere sphere) {
    return sphere.intersects(this);
  }

  public boolean intersects(@NonNull Ray ray) {
    Vector3d t0 = min.subtract(ray.origin).multiply(ray.invDir);
    Vector3d t1 = max.subtract(ray.origin).multiply(ray.invDir);
    return Vector3d.maxComponent(t0.min(t1)) <= Vector3d.minComponent(t0.max(t1));
  }

  @Override
  public @NonNull Vector3d position() {
    return min.add(max.subtract(min).multiply(0.5));
  }

  @Override
  public @NonNull AABB at(@NonNull Vector3d point) {
    Vector3d halfExtends = halfExtents();
    return new AABB(point.add(halfExtends.negate()), point.add(halfExtends));
  }

  @Override
  public @NonNull Vector3d halfExtents() {
    return max.subtract(min).multiply(0.5).abs();
  }

  @Override
  public boolean contains(@NonNull Vector3d point) {
    return (point.getX() >= min.getX() && point.getX() <= max.getX()) &&
      (point.getY() >= min.getY() && point.getY() <= max.getY()) &&
      (point.getZ() >= min.getZ() && point.getZ() <= max.getZ());
  }
}
