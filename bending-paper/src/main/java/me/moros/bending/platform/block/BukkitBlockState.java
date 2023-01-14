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

import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.property.Property;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitBlockState implements BlockState {
  private final BlockData handle;
  private final BlockType type;

  private BukkitBlockState(BlockData newData, BlockType type) {
    this.handle = newData;
    this.type = type;
  }

  public BukkitBlockState(BlockData handle) {
    this(handle.clone(), PlatformAdapter.BLOCK_MATERIAL_INDEX.keyOrThrow(handle.getMaterial()));
  }

  public BlockData handle() {
    return handle;
  }

  @Override
  public BlockType type() {
    return type;
  }

  @Override
  public boolean matches(BlockState other) {
    return other instanceof BukkitBlockState b && handle().matches(b.handle());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Comparable<V>> @Nullable V property(Property<V> property) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    return bukkitProperty == null ? null : (V) bukkitProperty.get(handle());
  }

  @Override
  public <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    if (bukkitProperty != null) {
      var result = bukkitProperty.set(handle(), value);
      if (result != null) {
        return new BukkitBlockState(result, type());
      }
    }
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BukkitBlockState other) {
      return handle().equals(other.handle());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
