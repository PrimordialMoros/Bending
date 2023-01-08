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

package me.moros.bending.platform.block;

import java.util.stream.Stream;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.platform.entity.Metadatable;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.adapter.Adapters;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public record Block(World world, int blockX, int blockY, int blockZ) implements Position, Metadatable {
  public Block(World world, Position position) {
    this(world, position.blockX(), position.blockY(), position.blockZ());
  }

  public Block offset(Position offset) {
    return offset(offset, 1);
  }

  public Block offset(Position offset, int m) {
    return offset(m * offset.blockX(), m * offset.blockY(), m * offset.blockZ());
  }

  public Block offset(int dx, int dy, int dz) {
    return new Block(world(), blockX + dx, blockY + dy, blockZ + dz);
  }

  public BlockType type() {
    return world().getBlockType(this);
  }

  public boolean setType(BlockType type) {
    return setState(type.defaultState());
  }

  public BlockState state() {
    return world().getBlockState(this);
  }

  public boolean setState(BlockState state) {
    return NativeAdapter.instance().setBlockFast(this, state);
  }

  public AABB bounds() {
    return world().blockBounds(this);
  }

  @Override
  public double x() {
    return blockX;
  }

  @Override
  public double y() {
    return blockY;
  }

  @Override
  public double z() {
    return blockZ;
  }

  @Override
  public Adapters<? extends Position> adapters() {
    return Adapters.vector3i();
  }

  @Override
  public boolean hasMetadata(Key key) {
    return world().hasMetadata(this, key);
  }

  @Override
  public <T> Stream<T> metadata(Key key, Class<T> type) {
    return world().metadata(this, key, type);
  }

  @Override
  public void addMetadata(Key key, @Nullable Object object) {
    world().addMetadata(this, key, object);
  }

  @Override
  public void removeMetadata(Key key) {
    world().removeMetadata(this, key);
  }
}
