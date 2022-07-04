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

package me.moros.bending.raytrace;

import java.util.Objects;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.util.EntityUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RayTraceResult {
  @NonNull Vector3d position();

  boolean hit();

  interface BlockRayTrace extends RayTraceResult {
    @Nullable Block block();

    @Nullable BlockFace face();
  }

  interface EntityRayTrace extends RayTraceResult {
    @Nullable Entity entity();

    default @NonNull Vector3d entityCenterOrPosition() {
      Entity entity = entity();
      return entity == null ? position() : EntityUtil.entityCenter(entity);
    }

    default @NonNull Vector3d entityEyeLevelOrPosition() {
      if (entity() instanceof LivingEntity livingEntity) {
        return new Vector3d(livingEntity.getEyeLocation());
      }
      return position();
    }
  }

  interface CompositeResult extends EntityRayTrace, BlockRayTrace {
  }

  static @NonNull CompositeResult miss(@NonNull Vector3d position) {
    Objects.requireNonNull(position);
    return new RayTraceResultImpl(position, null, null, null);
  }

  static @NonNull CompositeResult hit(@NonNull Vector3d position, @NonNull Block block, @NonNull BlockFace face) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(block);
    Objects.requireNonNull(face);
    return new RayTraceResultImpl(position, block, face, null);
  }

  static @NonNull CompositeResult hit(@NonNull Vector3d position, @NonNull Entity entity) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(entity);
    return new RayTraceResultImpl(position, null, null, entity);
  }
}
