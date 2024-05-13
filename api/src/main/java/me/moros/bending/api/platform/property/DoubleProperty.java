/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.platform.property;

import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DoubleProperty extends SimpleProperty<Double> {
  private final double min;
  private final double max;

  DoubleProperty(String name, double min, double max) {
    super(DataKey.wrap(KeyUtil.simple(name), Double.class));
    this.min = min;
    this.max = max;
  }

  public double min() {
    return min;
  }

  public double max() {
    return max;
  }

  @Override
  public boolean isValidValue(@Nullable Double value) {
    return value != null && value >= min() && value <= max();
  }
}
