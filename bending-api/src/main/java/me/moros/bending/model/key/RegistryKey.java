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
import org.jetbrains.annotations.NotNull;

public final class RegistryKey<T> implements Key {
  private final Key key;
  private final Class<T> clazz;

  private RegistryKey(Key key, Class<T> clazz) {
    this.key = key;
    this.clazz = clazz;
  }

  @Override
  public String namespace() {
    return key.namespace();
  }

  @Override
  public String value() {
    return key.value();
  }

  @Override
  public @NotNull Key key() {
    return key;
  }

  public Class<T> type() {
    return clazz;
  }

  public @Nullable T cast(@Nullable Object value) {
    try {
      return clazz.cast(value);
    } catch (ClassCastException e) {
      return null;
    }
  }

  public static <T> RegistryKey<T> create(String namespace, Class<T> clazz) {
    return new RegistryKey<>(Key.create(namespace, "registry"), clazz);
  }
}
