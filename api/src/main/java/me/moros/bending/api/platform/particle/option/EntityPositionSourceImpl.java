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

package me.moros.bending.api.platform.particle.option;

import java.lang.ref.WeakReference;
import java.util.Optional;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;

record EntityPositionSourceImpl(WeakReference<Entity> entityRef, double yOffset) implements EntityPositionSource {
  @Override
  public Optional<Entity> entity() {
    return Optional.ofNullable(entityRef.get());
  }

  @Override
  public Optional<Vector3d> position() {
    return entity().map(e -> e.location().add(0, yOffset, 0));
  }
}
