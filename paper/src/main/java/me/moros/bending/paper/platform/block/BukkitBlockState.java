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

package me.moros.bending.paper.platform.block;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.property.Property;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.Nullable;

public class BukkitBlockState implements BlockState {
  private final BlockData handle;
  private final BlockType type;

  private BukkitBlockState(BlockData newData, BlockType type) {
    this.handle = newData;
    this.type = type;
  }

  public BukkitBlockState(BlockData handle) {
    this(handle.clone(), PlatformAdapter.fromBukkitBlock(handle.getMaterial()));
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

  @Override
  public <V extends Comparable<V>> @Nullable V property(Property<V> property) {
    return BukkitBlockStateProperties.PROPERTIES.getValue(property, handle());
  }

  @Override
  public <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value) {
    if (property.isValidValue(value)) {
      var provider = BukkitBlockStateProperties.PROPERTIES.getProvider(property);
      if (provider != null && provider.supports(handle())) {
        var copy = handle().clone();
        boolean result = provider.set(copy, value);
        if (result && !copy.equals(handle())) {
          return new BukkitBlockState(copy, type());
        }
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
