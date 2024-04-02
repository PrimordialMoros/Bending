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
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.util.Constants;
import org.checkerframework.checker.nullness.qual.Nullable;

record VarHandleConfigEntry(String name, Class<?> type, VarHandle handle) implements ConfigEntry {
  private Number get(Object parent) {
    return (Number) handle.get(parent);
  }

  @Override
  public void modify(Object parent, DoubleUnaryOperator operator, Consumer<Throwable> consumer) {
    double value = operator.applyAsDouble(get(parent).doubleValue());
    handle.set(parent, toNative(value));
  }

  @Override
  public AttributeValue asAttributeValue(Object parent, Attribute attribute, @Nullable DoubleUnaryOperator modifier) {
    Number baseNumber = get(parent);
    Number modifiedNumber = baseNumber;
    if (modifier != null) {
      double base = baseNumber.doubleValue();
      double modified = modifier.applyAsDouble(base);
      if (Math.abs(modified - base) < Constants.EPSILON) {
        modifiedNumber = toNative(modified);
      }
    }
    return AttributeValue.of(attribute, name(), baseNumber, modifiedNumber);
  }
}
