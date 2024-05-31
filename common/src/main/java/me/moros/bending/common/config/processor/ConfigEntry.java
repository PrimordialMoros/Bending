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

package me.moros.bending.common.config.processor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeValue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.util.NamingSchemes;

interface ConfigEntry {
  Map<Class<? extends Number>, DoubleFunction<Number>> CONVERTERS = Map.of(
    Double.class, x -> x,
    Integer.class, x -> (int) x,
    Long.class, x -> (long) x,
    double.class, x -> x,
    int.class, x -> (int) x,
    long.class, x -> (long) x
  );

  String name();

  Class<?> type();

  void modify(ConfigurationNode parent, DoubleUnaryOperator operator, Consumer<Throwable> consumer);

  AttributeValue asAttributeValue(ConfigurationNode parent, Attribute attribute, @Nullable DoubleUnaryOperator modifier);

  default Number toNative(double value) {
    return CONVERTERS.getOrDefault(type(), x -> x).apply(value);
  }

  static ConfigEntry fromNode(String name, Class<?> type) {
    return new SimpleConfigEntry(NamingSchemes.LOWER_CASE_DASHED.coerce(name), type);
  }
}
