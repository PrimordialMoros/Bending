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
 * Axis aligned bounding box.
 */
public class AABB implements Collider {
  public static final AABB DUMMY_COLLIDER = new DummyCollider();
  public static final AABB BLOCK_BOUNDS = new AABB(Vector3d.ZERO, Vector3d.ONE);
  public static final AABB EXPANDED_BLOCK_BOUNDS = BLOCK_BOUNDS.grow(new Vector3d(0.4, 0.4, 0.4));

  public final Vector3d min;
  public final Vector3d max;

  public AABB(Vector3d min, Vector3d max) {
    this.min = min;
    this.max = max;
  }

  /**
   * Calculate an AABB by expanding this instance by the given amount in each component.
   * The expansion is uniform and will affect both the min and max points.
   * For example, expanding {@link #BLOCK_BOUNDS} (1x1x1 box) by {@link Vector3d#ONE} will result in a 3x3x3 box.
   * @param diff the amount to expand
   * @return the expanded AABB
   */
  public AABB grow(Vector3d diff) {
    return new AABB(min.subtract(diff), max.add(diff));
  }

  boolean _intersects(AABB other) {
    return (max.x() > other.min.x() && min.x() < other.max.x() &&
      max.y() > other.min.y() && min.y() < other.max.y() &&
      max.z() > other.min.z() && min.z() < other.max.z());
  }

  @Override
  public Vector3d position() {
    return min.add(max.subtract(min).multiply(0.5));
  }

  @Override
  public AABB at(Vector3d point) {
    Vector3d halfExtents = halfExtents();
    return new AABB(point.add(halfExtents.negate()), point.add(halfExtents));
  }

  @Override
  public Vector3d halfExtents() {
    return max.subtract(min).multiply(0.5).abs();
  }

  @Override
  public boolean contains(Vector3d point) {
    return (point.x() >= min.x() && point.x() <= max.x()) &&
      (point.y() >= min.y() && point.y() <= max.y()) &&
      (point.z() >= min.z() && point.z() <= max.z());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AABB other = (AABB) obj;
    return min.equals(other.min) && max.equals(other.max);
  }

  @Override
  public int hashCode() {
    int result = min.hashCode();
    result = 31 * result + max.hashCode();
    return result;
  }

  /**
   * Get a dummy AABB collider.
   * @return a dummy collider
   */
  public static AABB dummy() {
    return DUMMY_COLLIDER;
  }

  private static final class DummyCollider extends AABB {
    private DummyCollider() {
      super(Vector3d.ZERO, Vector3d.ZERO);
    }

    @Override
    public AABB grow(Vector3d diff) {
      return this;
    }

    @Override
    public boolean intersects(Collider other) {
      return false;
    }

    @Override
    public Vector3d position() {
      return Vector3d.ZERO;
    }

    @Override
    public AABB at(Vector3d point) {
      return this;
    }

    @Override
    public Vector3d halfExtents() {
      return Vector3d.ZERO;
    }

    @Override
    public boolean contains(Vector3d point) {
      return false;
    }
  }
}
