/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.collision.raytrace;

import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;

record ContextImpl(Vector3d origin, Vector3d endPoint, double range, double raySize, boolean ignoreLiquids,
                   boolean ignorePassable, Set<Vector3i> ignore, Predicate<Entity> entityPredicate) implements Context {
  @Override
  public boolean ignore(int x, int y, int z) {
    return ignore.contains(Vector3i.of(x, y, z));
  }
}
