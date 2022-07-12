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

package me.moros.bending.model.key;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class RegistryKey<T> implements Key {
  private final Key key;
  private final Class<T> type;

  private RegistryKey(Key key, Class<T> type) {
    this.key = key;
    this.type = type;
  }

  @Override
  public String namespace() {
    return key.namespace();
  }

  @Override
  public String value() {
    return key.value();
  }

  public Class<T> type() {
    return type;
  }

  public @Nullable T cast(@Nullable Object value) {
    try {
      return type.cast(value);
    } catch (ClassCastException e) {
      return null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof RegistryKey other) {
      return this.key.equals(other.key) && this.type == other.type;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = (31 * result) + type.hashCode();
    return result;
  }

  public static <T> RegistryKey<T> create(String namespace, Class<T> clazz) {
    return new RegistryKey<>(Key.create(namespace, "registry"), clazz);
  }
}
