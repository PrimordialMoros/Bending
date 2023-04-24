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

package me.moros.bending.api.util;

import java.util.function.Function;

import me.moros.bending.api.util.data.DataKey;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("PatternValidation")
public final class KeyUtil {
  public static final String BENDING_NAMESPACE = "bending";
  public static final Function<String, @Nullable Key> VANILLA_KEY_MAPPER = stringToKey(Key.MINECRAFT_NAMESPACE);
  private static final char DEFAULT_SEPARATOR = ':';

  private KeyUtil() {
  }

  public static Function<String, @Nullable Key> stringToKey(String namespace) {
    return input -> fromString(input, namespace);
  }

  public static Key vanilla(String value) {
    return Key.key(Key.MINECRAFT_NAMESPACE, value);
  }

  public static Key simple(String value) {
    return Key.key(BENDING_NAMESPACE, value);
  }

  public static <V> DataKey<V> data(String value, Class<V> type) {
    return DataKey.wrap(Key.key(BENDING_NAMESPACE, value), type);
  }

  private static @Nullable Key fromString(String string, String defNamespace) {
    int index = string.indexOf(DEFAULT_SEPARATOR);
    String namespace = index >= 1 ? string.substring(0, index) : defNamespace;
    String value = index >= 0 ? string.substring(index + 1) : string;
    if (Key.parseableNamespace(namespace) && Key.parseableValue(value)) {
      return Key.key(namespace, value);
    }
    return null;
  }
}
