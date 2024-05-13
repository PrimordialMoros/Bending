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

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Map.entry;
import static me.moros.bending.api.platform.block.BlockStateProperties.*;

final class FabricBlockStateProperties {
  private FabricBlockStateProperties() {
  }

  private static final Map<me.moros.bending.api.platform.property.Property<?>, Property<?>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(DRAG, BlockStateProperties.DRAG),
      entry(LIT, BlockStateProperties.LIT),
      entry(OPEN, BlockStateProperties.OPEN),
      entry(WATERLOGGED, BlockStateProperties.WATERLOGGED),
      entry(LAYERS, BlockStateProperties.LAYERS),
      entry(LEVEL, BlockStateProperties.LEVEL)
    );
  }

  @SuppressWarnings("unchecked")
  static <V extends Comparable<V>> @Nullable Property<V> find(me.moros.bending.api.platform.property.Property<V> property) {
    return (Property<V>) FabricBlockStateProperties.PROPERTIES.get(property);
  }
}
