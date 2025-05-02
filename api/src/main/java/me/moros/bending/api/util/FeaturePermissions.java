/*
 * Copyright 2020-2025 Moros
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

public final class FeaturePermissions {
  private FeaturePermissions() {
  }

  public static final String METAL = create("metal");
  public static final String LAVA = create("lava");
  public static final String BLUE_FIRE = create("bluefire");
  public static final String BOARD = create("board");
  @Deprecated(forRemoval = true, since = "3.12.0")
  public static final String OVERRIDE_LOCK = create("admin.overridelock");

  private static String create(String node) {
    return "bending." + node;
  }
}
