/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.fabric.platform.block;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.property.Property;
import me.moros.bending.fabric.platform.PlatformAdapter;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FabricBlockState(net.minecraft.world.level.block.state.BlockState handle,
                               BlockType type) implements BlockState {
  public FabricBlockState(net.minecraft.world.level.block.state.BlockState handle) {
    this(handle, PlatformAdapter.fromFabricBlock(handle.getBlock()));
  }

  @Override
  public boolean matches(BlockState other) {
    return other instanceof FabricBlockState b && handle().equals(b.handle()); // Potentially different behaviour
  }

  @Override
  public <V extends Comparable<V>> @Nullable V property(Property<V> property) {
    var vanillaProperty = FabricBlockStateProperties.find(property);
    return vanillaProperty == null ? null : handle().getOptionalValue(vanillaProperty).orElse(null);
  }

  @Override
  public <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value) {
    var vanillaProperty = FabricBlockStateProperties.find(property);
    if (vanillaProperty != null) {
      var state = handle().trySetValue(vanillaProperty, value);
      if (state != handle()) {
        return new FabricBlockState(state, type());
      }
    }
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof FabricBlockState other) {
      return handle().equals(other.handle());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
