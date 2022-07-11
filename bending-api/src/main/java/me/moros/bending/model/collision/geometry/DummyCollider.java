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
 * Dummy {@link AABB} collider for passable blocks
 */
public final class DummyCollider extends AABB {
  public DummyCollider() {
    super(Vector3d.ZERO, Vector3d.ZERO);
  }

  @Override
  public AABB grow(Vector3d diff) {
    return this;
  }

  @Override
  public boolean intersects(Collider collider) {
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
