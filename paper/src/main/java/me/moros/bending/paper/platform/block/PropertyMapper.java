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

package me.moros.bending.paper.platform.block;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import me.moros.bending.api.platform.property.Property;
import me.moros.bending.api.platform.property.StateProperty;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.block.data.type.Snow;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Map.entry;

// dirty solution till paper implements property api
final class PropertyMapper {
  static final Map<Property<?>, BukkitProperty<? extends BlockData, ? extends Comparable<?>>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(StateProperty.DRAG, boolProp(BubbleColumn.class, BubbleColumn::isDrag, BubbleColumn::setDrag)),
      entry(StateProperty.LIT, boolProp(Lightable.class, Lightable::isLit, Lightable::setLit)),
      entry(StateProperty.OPEN, boolProp(Openable.class, Openable::isOpen, Openable::setOpen)),
      entry(StateProperty.WATERLOGGED, boolProp(Waterlogged.class, Waterlogged::isWaterlogged, Waterlogged::setWaterlogged)),
      entry(StateProperty.LAYERS, intProp(Snow.class, Snow::getLayers, Snow::setLayers)),
      entry(StateProperty.LEVEL, intProp(Levelled.class, Levelled::getLevel, Levelled::setLevel))
    );
  }

  static <B extends BlockData> BukkitProperty<B, Boolean> boolProp(Class<B> type, Function<B, Boolean> getter, BiConsumer<B, Boolean> setter) {
    return new BukkitProperty<>(type, getter, setter);
  }

  static <B extends BlockData> BukkitProperty<B, Integer> intProp(Class<B> type, Function<B, Integer> getter, BiConsumer<B, Integer> setter) {
    return new BukkitProperty<>(type, getter, setter);
  }

  record BukkitProperty<B extends BlockData, V extends Comparable<V>>(Class<B> type, Function<B, V> getter,
                                                                      BiConsumer<B, V> setter) {
    @Nullable V get(BlockData data) {
      if (type.isInstance(data)) {
        return getter.apply(type.cast(data));
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable BlockData set(BlockData data, Object value) {
      if (type.isInstance(data)) {
        var clone = data.clone();
        setter.accept(type.cast(clone), (V) value);
        return clone;
      }
      return null;
    }
  }
}
