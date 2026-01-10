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

package me.moros.bending.api.util.data;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

class SimpleDataContainer implements DataContainer {
  private final Map<DataKey<?>, Object> data;

  SimpleDataContainer(Map<DataKey<?>, Object> data) {
    this.data = data;
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return Optional.ofNullable(cast(key.type(), data.get(key)));
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    data.put(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    data.remove(key);
  }

  @Override
  public <T> boolean canEdit(DataKey<T> key) {
    return true;
  }

  @Override
  public <T extends Enum<T>> T toggle(DataKey<T> key, T defaultValue) {
    T oldValue = cast(key.type(), data.computeIfAbsent(key, k -> defaultValue));
    if (oldValue != null && !canEdit(key)) {
      return oldValue;
    }
    T newValue = toggle(oldValue == null ? defaultValue : oldValue);
    add(key, newValue);
    return newValue;
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  private <T extends Enum<T>> T toggle(T oldValue) {
    T[] values = oldValue.getDeclaringClass().getEnumConstants();
    int index = (oldValue.ordinal() + 1) % values.length;
    return values[index];
  }

  private <T> @Nullable T cast(Class<T> type, Object value) {
    if (type.isInstance(value)) {
      return type.cast(value);
    }
    return null;
  }
}
