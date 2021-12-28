/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to handle chat related functionality.
 */
public final class ChatUtil {
  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");
  private static final Pattern SPACE = Pattern.compile(" ");
  private static final String[] CHAT_CODES;

  static {
    var arr = ChatColor.values();
    CHAT_CODES = new String[arr.length];
    for (ChatColor color : arr) {
      CHAT_CODES[color.ordinal()] = color.toString();
    }
  }

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
    String output = NON_ALPHANUMERIC.matcher(input).replaceAll("").toLowerCase(Locale.ROOT);
    return output.length() > 16 ? output.substring(0, 16) : output;
  }

  public static @NonNull String generateInvisibleString(int slot) {
    String hidden = CHAT_CODES[slot % CHAT_CODES.length];
    return slot <= CHAT_CODES.length ? hidden : hidden + generateInvisibleString(slot - CHAT_CODES.length);
  }

  public static @NonNull List<@NonNull String> wrap(@NonNull String str, int wrapLength) {
    wrapLength = Math.max(1, wrapLength);
    final int length = str.length();
    int offset = 0;
    List<String> lines = new ArrayList<>(length / wrapLength);
    while (offset < length) {
      int spaceToWrapAt = -1;
      Matcher matcher = SPACE.matcher(str.substring(offset, Math.min(offset + wrapLength + 1, length)));
      if (matcher.find()) {
        if (matcher.start() == 0) {
          offset += matcher.end();
          continue;
        }
        spaceToWrapAt = matcher.start() + offset;
      }
      if (length - offset <= wrapLength) {
        break;
      }
      while (matcher.find()) {
        spaceToWrapAt = matcher.start() + offset;
      }
      if (spaceToWrapAt >= offset) {
        lines.add(str.substring(offset, spaceToWrapAt));
        offset = spaceToWrapAt + 1;
      } else {
        matcher = SPACE.matcher(str.substring(offset + wrapLength));
        if (matcher.find()) {
          spaceToWrapAt = matcher.start() + offset + wrapLength;
        }
        if (spaceToWrapAt >= 0) {
          lines.add(str.substring(offset, spaceToWrapAt));
          offset = spaceToWrapAt + 1;
        } else {
          lines.add(str.substring(offset, length));
          offset = length;
        }
      }
    }
    lines.add(str.substring(offset, length));
    return lines;
  }
}
