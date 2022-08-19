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

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An identifying object used to fetch and/or store unique objects.
 * A key consists of a namespace and a value.
 */
public interface Key extends Keyed, Namespaced {
  /**
   * Gets the value.
   * @return the value
   */
  String value();

  default Key key() {
    return this;
  }

  /**
   * Creates a key.
   * @param namespace the namespace
   * @param value the value
   * @return the key
   * @throws IllegalArgumentException if the namespace or value contains an invalid character
   */
  static Key create(String namespace, String value) {
    Key key = tryCreate(namespace, value);
    if (key == null) {
      throw new IllegalArgumentException("Invalid key namespace or value");
    }
    return key;
  }

  /**
   * Attempts to create a key.
   * @param namespace the namespace
   * @param value the value
   * @return the key or null if the namespace and/or value contained an invalid character
   */
  static @Nullable Key tryCreate(String namespace, String value) {
    String validNamespace = validate(namespace);
    String validValue = validate(value);
    return (validNamespace != null && validValue != null) ? new KeyImpl(validNamespace, validValue) : null;
  }

  private static @Nullable String validate(String input) {
    String lowerCase = input.toLowerCase(Locale.ROOT);
    return isValidString(lowerCase) ? lowerCase : null;
  }

  private static boolean isValidString(String input) {
    return input.chars().allMatch(Key::isValidChar);
  }

  private static boolean isValidChar(int chr) {
    return chr == '_' || chr == '-' || (chr >= 'a' && chr <= 'z') || (chr >= '0' && chr <= '9') || chr == '.';
  }
}
