/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to handle chat related functionality.
 */
public final class ChatUtil {
  private static final Pattern NON_ALPHABETICAL = Pattern.compile("[^A-Za-z]");

  private ChatUtil() {
  }

  /**
   * Strip input of all non alphabetical values and limit to 16 characters long.
   * This is used for preset names mainly.
   * @param input input the input string to sanitize
   * @return the sanitized output string
   */
  public static @NonNull String sanitizeInput(@Nullable String input) {
    if (input == null) {
      return "";
    }
    String output = NON_ALPHABETICAL.matcher(input).replaceAll("").toLowerCase();
    return output.length() > 16 ? output.substring(0, 16) : output;
  }
}
