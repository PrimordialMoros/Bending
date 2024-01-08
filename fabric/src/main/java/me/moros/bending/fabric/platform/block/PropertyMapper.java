/*
 * Copyright 2020-2024 Moros
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

import java.util.Map;

import me.moros.bending.api.platform.property.Property;
import me.moros.bending.api.platform.property.StateProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Map.entry;

final class PropertyMapper {
  static final Map<Property<?>, net.minecraft.world.level.block.state.properties.Property<?>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(StateProperty.DRAG, BlockStateProperties.DRAG),
      entry(StateProperty.LIT, BlockStateProperties.LIT),
      entry(StateProperty.OPEN, BlockStateProperties.OPEN),
      entry(StateProperty.WATERLOGGED, BlockStateProperties.WATERLOGGED),
      entry(StateProperty.LAYERS, BlockStateProperties.LAYERS),
      entry(StateProperty.LEVEL, BlockStateProperties.LEVEL)
    );
  }

  @SuppressWarnings("unchecked")
  static <V extends Comparable<V>> net.minecraft.world.level.block.state.properties.@Nullable Property<V> find(Property<V> property) {
    return (net.minecraft.world.level.block.state.properties.Property<V>) PropertyMapper.PROPERTIES.get(property);
  }
}
