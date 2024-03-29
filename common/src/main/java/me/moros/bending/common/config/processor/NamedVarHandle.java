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

import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeValue;
import org.checkerframework.checker.nullness.qual.Nullable;

public record NamedVarHandle(String name, Class<?> type, VarHandle handle) {
  private static final Map<Class<? extends Number>, DoubleFunction<Number>> CONVERTERS = Map.of(
    Double.class, x -> x,
    Integer.class, x -> (int) x,
    Long.class, x -> (long) x,
    double.class, x -> x,
    int.class, x -> (int) x,
    long.class, x -> (long) x
  );

  private Number toNative(double value) {
    return CONVERTERS.getOrDefault(type, x -> x).apply(value);
  }

  private Number get(Configurable instance) {
    return (Number) handle.get(instance);
  }

  private void set(Configurable instance, double value) {
    handle.set(instance, toNative(value));
  }

  public void modify(Configurable instance, DoubleUnaryOperator operator) {
    set(instance, operator.applyAsDouble(get(instance).doubleValue()));
  }

  public AttributeValue asAttributeValue(Configurable instance, Attribute attribute, @Nullable ModificationMatrix matrix) {
    Number baseValue = get(instance);
    Number finalValue = baseValue;
    if (matrix != null) {
      double base = baseValue.doubleValue();
      double modified = matrix.applyAsDouble(base);
      if (base != modified) {
        finalValue = toNative(modified);
      }
    }
    return AttributeValue.of(attribute, name(), baseValue, finalValue);
  }
}
