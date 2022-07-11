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

public final class KeyValidator {
  private KeyValidator() {
  }

  public static String validate(String input) {
    String lowerCase = input.toLowerCase(Locale.ROOT);
    if (!isValidString(lowerCase)) {
      throw new RuntimeException("Invalid key namespace or value " + lowerCase);
    }
    return lowerCase;
  }

  public static boolean isValidString(String input) {
    return input.chars().allMatch(KeyValidator::isValidChar);
  }

  public static boolean isValidChar(int chr) {
    return chr == '_' || chr == '-' || (chr >= 'a' && chr <= 'z') || (chr >= '0' && chr <= '9') || chr == '.';
  }
}
