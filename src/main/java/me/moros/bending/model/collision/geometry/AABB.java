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
 * Axis aligned bounding box
 */
public class AABB implements Collider {
  public static final AABB PLAYER_BOUNDS = new AABB(new Vector3(-0.3, 0.0, -0.3), new Vector3(0.3, 1.8, 0.3));
  public static final AABB BLOCK_BOUNDS = new AABB(Vector3.ZERO, Vector3.ONE);
  public static final AABB EXPANDED_BLOCK_BOUNDS = BLOCK_BOUNDS.grow(new Vector3(0.4, 0.4, 0.4));

  public final Vector3 min;
  public final Vector3 max;

  public AABB(@NonNull Vector3 min, @NonNull Vector3 max) {
    this.min = min;
    this.max = max;
  }

  public @NonNull AABB grow(@NonNull Vector3 diff) {
    return new AABB(min.subtract(diff), max.add(diff));
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
      return collider.intersects(this);
    } else if (collider instanceof Disk) {
      return collider.intersects(this);
    }
    return false;
  }

  private boolean intersects(@NonNull AABB other) {
    return (max.x > other.min.x && min.x < other.max.x &&
      max.y > other.min.y && min.y < other.max.y &&
      max.z > other.min.z && min.z < other.max.z);
  }

  private boolean intersects(@NonNull Sphere sphere) {
    return sphere.intersects(this);
  }

  public boolean intersects(@NonNull Ray ray) {
    Vector3 t0 = min.subtract(ray.origin).multiply(ray.invDir);
    Vector3 t1 = max.subtract(ray.origin).multiply(ray.invDir);
    return Vector3.maxComponent(t0.min(t1)) <= Vector3.minComponent(t0.max(t1));
  }

  @Override
  public @NonNull Vector3 position() {
    return min.add(max.subtract(min).multiply(0.5));
  }

  @Override
  public @NonNull AABB at(@NonNull Vector3 point) {
    Vector3 halfExtends = halfExtents();
    return new AABB(point.add(halfExtends.negate()), point.add(halfExtends));
  }

  @Override
  public @NonNull Vector3 halfExtents() {
    return max.subtract(min).multiply(0.5).abs();
  }

  @Override
  public boolean contains(@NonNull Vector3 point) {
    return (point.x >= min.x && point.x <= max.x) &&
      (point.y >= min.y && point.y <= max.y) &&
      (point.z >= min.z && point.z <= max.z);
  }
}
