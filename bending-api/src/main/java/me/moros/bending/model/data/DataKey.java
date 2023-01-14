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

package me.moros.bending.model.data;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Key implementation that also holds the type of data that can be associated with it.
 * @param <T> the type of data
 */
public final class DataKey<T> implements Key {
  private final Key key;
  private final Class<T> type;

  private DataKey(Key key, Class<T> type) {
    this.key = key;
    this.type = type;
  }

  @Override
  public @NonNull String namespace() {
    return key.namespace();
  }

  @Override
  public @NonNull String value() {
    return key.value();
  }

  @Override
  public @NonNull String asString() {
    return key.asString();
  }

  /**
   * Get the type of data this key can be associated with.
   * @return the type of data
   */
  public Class<T> type() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof DataKey<?> other) {
      return key.equals(other.key) && type == other.type;
    }
    return false;
  }

  private int hashcode;

  @Override
  public int hashCode() {
    if (hashcode == 0) {
      hashcode = (31 * key.hashCode()) + type.hashCode();
    }
    return hashcode;
  }

  /**
   * Create a new bending key by wrapping an existing key.
   * @param key the key to wrap
   * @param clazz the type
   * @param <T> the type of data
   * @return the constructed bending key
   */
  @SuppressWarnings("unchecked")
  public static <T> DataKey<T> wrap(Key key, Class<T> clazz) {
    if (key instanceof DataKey<?> k && k.type() == clazz) {
      return (DataKey<T>) k;
    }
    return new DataKey<>(key, clazz);
  }
}
