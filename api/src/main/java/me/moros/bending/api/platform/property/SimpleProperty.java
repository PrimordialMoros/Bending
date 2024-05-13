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

package me.moros.bending.api.platform.property;

import me.moros.bending.api.util.data.DataKey;

sealed class SimpleProperty<T> implements Property<T> permits BooleanProperty, DoubleProperty, FloatProperty, IntegerProperty {
  private final DataKey<T> key;

  SimpleProperty(DataKey<T> key) {
    this.key = key;
  }

  public final DataKey<T> dataKey() {
    return key;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Property<?> other) {
      return key.equals(other.key());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return key.hashCode();
  }
}
