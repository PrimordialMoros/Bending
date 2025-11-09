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

package me.moros.bending.api.platform.property;

import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import org.jspecify.annotations.Nullable;

public sealed interface Property<T> extends DataKeyed<T> permits SimpleProperty {
  default boolean isValidValue(@Nullable T value) {
    return value != null;
  }

  static BooleanProperty boolProp(String name) {
    return new BooleanProperty(name);
  }

  static DoubleProperty doubleProp(String name) {
    return doubleProp(name, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  static DoubleProperty doubleProp(String name, double min, double max) {
    if (min > max) {
      throw new IllegalArgumentException("Invalid range. Min: " + min + ", Max: " + max);
    }
    return new DoubleProperty(name, min, max);
  }

  static FloatProperty floatProp(String name) {
    return floatProp(name, -Float.MAX_VALUE, Float.MAX_VALUE);
  }

  static FloatProperty floatProp(String name, float min, float max) {
    if (min > max) {
      throw new IllegalArgumentException("Invalid range. Min: " + min + ", Max: " + max);
    }
    return new FloatProperty(name, min, max);
  }

  static IntegerProperty intProp(String name) {
    return intProp(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  static IntegerProperty intProp(String name, int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("Invalid range. Min: " + min + ", Max: " + max);
    }
    return new IntegerProperty(name, min, max);
  }

  static <T> Property<T> prop(String name, Class<T> type) {
    return new SimpleProperty<>(DataKey.wrap(KeyUtil.simple(name), type));
  }
}
