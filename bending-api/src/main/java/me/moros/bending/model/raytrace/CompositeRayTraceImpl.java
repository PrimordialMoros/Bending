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

package me.moros.bending.model.raytrace;

import me.moros.bending.model.math.Vector3d;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CompositeRayTraceImpl implements CompositeRayTrace {
  private final Vector3d position;
  private final Block block;
  private final BlockFace face;
  private final Entity entity;
  private final boolean hit;

  CompositeRayTraceImpl(Vector3d position, @Nullable Block block, @Nullable BlockFace face, @Nullable Entity entity) {
    this.position = position;
    this.block = block;
    this.face = face;
    this.entity = entity;
    this.hit = block != null || entity != null;
  }

  @Override
  public Vector3d position() {
    return position;
  }

  @Override
  public @Nullable Block block() {
    return block;
  }

  @Override
  public @Nullable BlockFace face() {
    return face;
  }

  @Override
  public @Nullable Entity entity() {
    return entity;
  }

  @Override
  public boolean hit() {
    return hit;
  }
}
