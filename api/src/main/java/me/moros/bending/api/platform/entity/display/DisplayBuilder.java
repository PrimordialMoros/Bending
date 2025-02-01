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

package me.moros.bending.api.platform.entity.display;

import net.kyori.adventure.util.RGBLike;

public sealed interface DisplayBuilder<V, T extends DisplayBuilder<V, T>> permits AbstractDisplayBuilder {
  V data();

  T data(V data);

  float width();

  T width(float width);

  float height();

  T height(float height);

  float viewRange();

  T viewRange(float viewRange);

  float shadowRadius();

  T shadowRadius(float shadowRadius);

  float shadowStrength();

  T shadowStrength(float shadowStrength);

  int interpolationDelay();

  T interpolationDelay(int interpolationDelay);

  int transformationInterpolationDuration();

  T transformationInterpolationDuration(int transformationInterpolationDuration);

  int positionInterpolationDuration();

  T positionInterpolationDuration(int positionInterpolationDuration);

  int brightness();

  default T brightness(int blockLight, int skyLight) {
    return brightness(Math.clamp(blockLight, 0, 15) << 4 | Math.clamp(skyLight, 0, 15) << 20);
  }

  T brightness(int brightness);

  int glowColor();

  T glowColor(RGBLike color);

  T glowColor(int argb);

  Billboard billboard();

  T billboard(Billboard billboard);

  Transformation transformation();

  T transformation(Transformation transformation);

  Display<? super V> build();
}
