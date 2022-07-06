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

package me.moros.bending.adapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class VersionUtil {
  private static NMSVersion nmsVersion;

  private VersionUtil() {
  }

  public static @NonNull NMSVersion nmsVersion() {
    if (nmsVersion == null) {
      String name = Bukkit.getServer().getClass().getName();
      String[] parts = name.split("\\.");
      if (parts.length > 3) {
        return nmsVersion = NMSVersion.fromString(parts[3]);
      }
      throw new RuntimeException("Unknown server version format!");
    }
    return nmsVersion;
  }

  public record NMSVersion(int major, int minor, int release) implements Comparable<NMSVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)_(\\d+)_R(\\d+)");

    private static NMSVersion fromString(String string) {
      Matcher matcher = VERSION_PATTERN.matcher(string);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid version format: " + string);
      }
      return new NMSVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    @Override
    public String toString() {
      return "v" + major + "_" + minor + "_R" + release;
    }

    @Override
    public int compareTo(NMSVersion o) {
      if (major < o.major) {
        return -1;
      } else if (major > o.major) {
        return 1;
      } else {
        if (minor < o.minor) {
          return -1;
        } else if (minor > o.minor) {
          return 1;
        } else {
          return Integer.compare(release, o.release);
        }
      }
    }
  }
}
