/*
 * Copyright 2020-2026 Moros
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
import org.jspecify.annotations.Nullable;

public final class FloatProperty extends SimpleProperty<Float> {
  private final float min;
  private final float max;

  FloatProperty(String name, float min, float max) {
    super(DataKey.wrap(KeyUtil.simple(name), Float.class));
    this.min = min;
    this.max = max;
  }

  public float min() {
    return min;
  }

  public float max() {
    return max;
  }

  @Override
  public boolean isValidValue(@Nullable Float value) {
    return value != null && value >= min() && value <= max();
  }
}
