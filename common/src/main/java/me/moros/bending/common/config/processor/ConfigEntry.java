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

package me.moros.bending.common.config.processor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.util.Constants;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.NamingSchemes;

record ConfigEntry(String name, Class<?> type) {
  private static final Map<Class<? extends Number>, DoubleFunction<Number>> CONVERTERS = Map.of(
    Double.class, x -> x,
    Integer.class, x -> (int) x,
    Long.class, x -> (long) x,
    double.class, x -> x,
    int.class, x -> (int) x,
    long.class, x -> (long) x
  );

  ConfigEntry(Field field) {
    this(NamingSchemes.LOWER_CASE_DASHED.coerce(field.getName()), field.getType());
  }

  private ConfigurationNode node(ConfigurationNode parent) {
    return parent.node(name);
  }

  void modify(ConfigurationNode parent, DoubleUnaryOperator operator, Consumer<Throwable> consumer) {
    ConfigurationNode node = node(parent);
    double baseValue = node.getDouble();
    try {
      node.set(toNative(operator.applyAsDouble(baseValue)));
    } catch (SerializationException e) {
      consumer.accept(e);
    }
  }

  AttributeValue asAttributeValue(ConfigurationNode parent, Attribute attribute, @Nullable DoubleUnaryOperator modifier) {
    double base = node(parent).getDouble();
    Number modifiedNumber = base;
    if (modifier != null) {
      double modified = modifier.applyAsDouble(base);
      if (Math.abs(modified - base) > Constants.EPSILON) {
        modifiedNumber = toNative(modified);
      }
    }
    return AttributeValue.of(attribute, name, base, modifiedNumber);
  }

  private Number toNative(double value) {
    return CONVERTERS.getOrDefault(type(), x -> x).apply(value);
  }
}
