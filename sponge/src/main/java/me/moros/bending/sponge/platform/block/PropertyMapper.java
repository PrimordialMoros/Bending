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

package me.moros.bending.sponge.platform.block;

import java.util.Map;

import me.moros.bending.api.platform.property.Property;
import me.moros.bending.api.platform.property.StateProperty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.state.BooleanStateProperties;
import org.spongepowered.api.state.IntegerStateProperties;

import static java.util.Map.entry;

final class PropertyMapper {
  static final Map<Property<?>, org.spongepowered.api.state.StateProperty<?>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(StateProperty.DRAG, BooleanStateProperties.property_DRAG()),
      entry(StateProperty.LIT, BooleanStateProperties.property_LIT()),
      entry(StateProperty.OPEN, BooleanStateProperties.property_OPEN()),
      entry(StateProperty.WATERLOGGED, BooleanStateProperties.property_WATERLOGGED()),
      entry(StateProperty.LAYERS, IntegerStateProperties.property_LAYERS()),
      entry(StateProperty.LEVEL, IntegerStateProperties.property_LEVEL())
    );
  }

  @SuppressWarnings("unchecked")
  static <V extends Comparable<V>> org.spongepowered.api.state.@Nullable StateProperty<V> find(Property<V> property) {
    return (org.spongepowered.api.state.StateProperty<V>) PropertyMapper.PROPERTIES.get(property);
  }
}
