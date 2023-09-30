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

package me.moros.bending.api.platform.entity.display;

import net.kyori.adventure.text.Component;

record TextDisplayImpl(
  Component data, float width, float height, float viewRange, float shadowRadius, float shadowStrength,
  int interpolationDelay, int transformationInterpolationDuration, int positionInterpolationDuration,
  int brightness, int glowColor, Billboard billboard, Transformation transformation,
  int lineWidth, int backgroundColor, byte opacity, TextFlags textFlags
) implements TextDisplay {
  TextDisplayImpl(TextDisplayBuilder builder) {
    this(builder.data(), builder.width(), builder.height(), builder.viewRange(),
      builder.shadowRadius(), builder.shadowStrength(),
      builder.interpolationDelay(), builder.transformationInterpolationDuration(), builder.positionInterpolationDuration(),
      builder.brightness(), builder.glowColor(), builder.billboard(), builder.transformation(),
      builder.lineWidth(), builder.backgroundColor(), builder.opacity(),
      new TextFlagsImpl(builder.hasShadow(), builder.isSeeThrough(), builder.hasDefaultBackground(), builder.alignment()));
  }

  @Override
  public TextDisplayBuilder toBuilder() {
    return new TextDisplayBuilder(this);
  }

  record TextFlagsImpl(boolean hasShadow, boolean isSeeThrough, boolean hasDefaultBackground,
                       Alignment alignment) implements TextFlags {
  }
}
