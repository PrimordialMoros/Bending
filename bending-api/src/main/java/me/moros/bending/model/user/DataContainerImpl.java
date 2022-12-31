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

package me.moros.bending.model.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.model.BendingKey;
import me.moros.bending.model.ExpiringSet;
import org.checkerframework.checker.nullness.qual.Nullable;

record DataContainerImpl(Map<BendingKey<?>, Object> data,
                         ExpiringSet<BendingKey<?>> cooldowns) implements DataContainer {
  DataContainerImpl() {
    this(new ConcurrentHashMap<>(), new ExpiringSet<>(500));
  }

  @Override
  public <T> boolean containsKey(BendingKey<T> key) {
    return data.containsKey(key);
  }

  @Override
  public <T> boolean canEdit(BendingKey<T> key) {
    return !cooldowns.contains(key);
  }

  @Override
  public <T> void put(BendingKey<T> key, T value) {
    cooldowns.add(key);
    data.put(key, value);
  }

  @Override
  public <T> @Nullable T remove(BendingKey<T> key) {
    return cast(key.type(), data.remove(key));
  }

  @Override
  public <T> @Nullable T get(BendingKey<T> key) {
    return cast(key.type(), data.get(key));
  }

  @Override
  public <T extends Enum<T>> T toggle(BendingKey<T> key, T defaultValue) {
    T oldValue = cast(key.type(), data.computeIfAbsent(key, k -> defaultValue));
    if (oldValue != null && !canEdit(key)) {
      return oldValue;
    }
    T newValue = toggle(oldValue == null ? defaultValue : oldValue);
    put(key, newValue);
    return newValue;
  }

  private <T extends Enum<T>> T toggle(T oldValue) {
    T[] values = oldValue.getDeclaringClass().getEnumConstants();
    int index = (oldValue.ordinal() + 1) % values.length;
    return values[index];
  }

  private <T> @Nullable T cast(Class<T> type, Object value) {
    try {
      return type.cast(value);
    } catch (ClassCastException e) {
      return null;
    }
  }
}
