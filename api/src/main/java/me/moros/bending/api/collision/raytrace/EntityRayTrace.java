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

package me.moros.bending.api.collision.raytrace;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a raytrace result that can hit an entity.
 */
public interface EntityRayTrace extends RayTrace {
  @Nullable Entity entity();

  default Vector3d entityCenterOrPosition() {
    Entity entity = entity();
    return entity == null ? position() : entity.center();
  }

  default Vector3d entityEyeLevelOrPosition() {
    if (entity() instanceof LivingEntity livingEntity) {
      return livingEntity.eyeLocation();
    }
    return position();
  }
}
