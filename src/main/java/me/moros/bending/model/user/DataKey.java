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

import me.moros.bending.Bending;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DataKey<E> implements Key {
  private final Key key;
  private final Class<E> clazz;

  private DataKey(String key, Class<E> clazz) {
    this.key = Key.key("bending", key);
    this.clazz = clazz;
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

  @Override
  public boolean equals(final Object other) {
    return key.equals(other);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  public @Nullable E cast(@Nullable Object value) {
    try {
      return clazz.cast(value);
    } catch (ClassCastException e) {
      Bending.logger().warn(e.getMessage(), e);
      return null;
    }
  }

  public static <E> @NonNull DataKey<E> of(@NonNull String key, @NonNull Class<E> clazz) {
    return new DataKey<>(key, clazz);
  }
}
