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
    var vanillaProperty = PropertyMapper.find(property);
    return vanillaProperty == null ? null : handle().getOptionalValue(vanillaProperty).orElse(null);
  }

  @Override
  public <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value) {
    var vanillaProperty = PropertyMapper.find(property);
    if (vanillaProperty != null && handle().hasProperty(vanillaProperty)) { // extra check, remove later
      var state = handle().setValue(vanillaProperty, value); // TODO replace with trySet
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
