/*
 * Copyright 2020-2022 Moros
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
import java.util.function.BiFunction;

import me.moros.bending.model.ExpiringSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DataContainer implements DataHolder {
  private final Map<DataKey<?>, Object> data;
  private final ExpiringSet<DataKey<?>> cooldowns;

  DataContainer() {
    data = new ConcurrentHashMap<>();
    cooldowns = new ExpiringSet<>(500);
  }

  @Override
  public <E> boolean containsKey(@NonNull DataKey<E> key) {
    return data.containsKey(key);
  }

  @Override
  public <E> boolean canEdit(@NonNull DataKey<E> key) {
    return !cooldowns.contains(key);
  }

  @Override
  public <E> boolean offer(@NonNull DataKey<E> key, @NonNull E value) {
    if (canEdit(key)) {
      put(key, value);
      return true;
    }
    return false;
  }

  @Override
  public <E> @Nullable E remove(@NonNull DataKey<E> key) {
    return key.cast(data.remove(key));
  }

  @Override
  public <E> @Nullable E get(@NonNull DataKey<E> key) {
    return key.cast(data.get(key));
  }

  @Override
  public <E> @NonNull E getOrDefault(@NonNull DataKey<E> key, @NonNull E defaultValue) {
    E oldValue = get(key);
    return oldValue != null ? oldValue : defaultValue;
  }

  @Override
  public <E> @NonNull E merge(@NonNull DataKey<E> key, @NonNull E value, @NonNull BiFunction<@NonNull ? super E, @NonNull ? super E, @NonNull ? extends E> remappingFunction) {
    E oldValue = get(key);
    E newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
    if (oldValue != null && !canEdit(key)) {
      return oldValue;
    }
    return put(key, newValue);
  }

  private <E> E put(DataKey<E> key, E value) {
    cooldowns.add(key);
    data.put(key, value);
    return value;
  }
}
