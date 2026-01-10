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

package me.moros.bending.sponge.platform.block;

import java.util.Map;

import me.moros.bending.api.platform.property.Property;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.state.BooleanStateProperties;
import org.spongepowered.api.state.IntegerStateProperties;
import org.spongepowered.api.state.StateProperty;

import static java.util.Map.entry;
import static me.moros.bending.api.platform.block.BlockStateProperties.*;

final class SpongeBlockStateProperties {
  private SpongeBlockStateProperties() {
  }

  private static final Map<Property<?>, StateProperty<?>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(DRAG, BooleanStateProperties.property_DRAG()),
      entry(LIT, BooleanStateProperties.property_LIT()),
      entry(OPEN, BooleanStateProperties.property_OPEN()),
      entry(WATERLOGGED, BooleanStateProperties.property_WATERLOGGED()),
      entry(LAYERS, IntegerStateProperties.property_LAYERS()),
      entry(LEVEL, IntegerStateProperties.property_LEVEL())
    );
  }

  @SuppressWarnings("unchecked")
  static <V extends Comparable<V>> @Nullable StateProperty<V> find(Property<V> property) {
    return (StateProperty<V>) SpongeBlockStateProperties.PROPERTIES.get(property);
  }
}
