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

package me.moros.bending.platform.property;

public final class IntegerProperty extends Property<Integer> {
  private final int min;
  private final int max;

  IntegerProperty(String name, int min, int max) {
    super(name, Integer.class);
    this.min = min;
    this.max = max;
    if (min < 0 || max <= min) {
      throw new IllegalArgumentException("Invalid range. Min: " + min + ", Max: " + max);
    }
  }

  public int min() {
    return this.min;
  }

  public int max() {
    return this.max;
  }
}
