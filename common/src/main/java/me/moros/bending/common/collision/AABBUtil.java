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

package me.moros.bending.common.collision;

import java.util.Collection;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.math.Vector3d;

public final class AABBUtil {
  private static final double MARGIN = 0.01;

  private AABBUtil() {
  }

  public static AABB combine(AABB first, AABB second) {
    return AABB.of(
      first.min().min(second.min()).subtract(MARGIN, MARGIN, MARGIN),
      first.max().max(second.max()).add(MARGIN, MARGIN, MARGIN)
    );
  }

  public static AABB combine(Collection<Collider> colliders) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;
    for (Collider collider : colliders) {
      AABB aabb = collider.outer();
      minX = Math.min(minX, aabb.min().x());
      minY = Math.min(minY, aabb.min().y());
      minZ = Math.min(minZ, aabb.min().z());
      maxX = Math.max(maxX, aabb.max().x());
      maxY = Math.max(maxY, aabb.max().y());
      maxZ = Math.max(maxZ, aabb.max().z());
    }
    return AABB.of(
      Vector3d.of(minX - MARGIN, minY - MARGIN, minZ - MARGIN),
      Vector3d.of(maxX + MARGIN, maxY + MARGIN, maxZ + MARGIN)
    );
  }
}
