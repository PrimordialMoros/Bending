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

import net.kyori.adventure.text.format.TextColor;

/**
 * Provides default colors for Bending.
 */
public interface ColorPalette {
  /**
   * Wheat
   */
  TextColor LINK_COLOR = TextColor.color(245, 222, 179);

  /**
   * Light gray
   */
  TextColor TEXT_COLOR = TextColor.color(224, 224, 224);

  /**
   * Amber
   */
  TextColor ACCENT = TextColor.color(255, 172, 18);

  /**
   * Dark cyan
   */
  TextColor HEADER = TextColor.color(38, 166, 154);

  /**
   * Dark gray
   */
  TextColor METAL = TextColor.color(97, 97, 97);

  /**
   * Green
   */
  TextColor SUCCESS = TextColor.color(0, 233, 10);

  /**
   * Mid gray
   */
  TextColor NEUTRAL = TextColor.color(158, 158, 158);

  /**
   * Yellow
   */
  TextColor WARN = TextColor.color(255, 255, 84);

  /**
   * Red
   */
  TextColor FAIL = TextColor.color(255, 84, 84);

  /**
   * Lighter gray
   */
  TextColor AIR = TextColor.color(189, 189, 189);

  /**
   * Mid blue
   */
  TextColor WATER = TextColor.color(33, 150, 243);

  /**
   * Dark green
   */
  TextColor EARTH = TextColor.color(10, 200, 20);

  /**
   * Dark red
   */
  TextColor FIRE = TextColor.color(195, 0, 0);

  /**
   * Dark purple
   */
  TextColor AVATAR = TextColor.color(128, 0, 128);
}
