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

package me.moros.bending.api.platform.entity.display;

import java.util.Objects;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.item.Item;
import net.kyori.adventure.text.Component;

public sealed interface Display<V> permits BlockDisplay, ItemDisplay, TextDisplay {
  V data();

  float width();

  float height();

  float viewRange();

  float shadowRadius();

  float shadowStrength();

  int interpolationDelay();

  int transformationInterpolationDuration();

  int positionInterpolationDuration();

  int brightness();

  int glowColor();

  Billboard billboard();

  Transformation transformation();

  DisplayBuilder<V, ?> toBuilder();

  static BlockDisplayBuilder block(BlockState data) {
    Objects.requireNonNull(data);
    return new BlockDisplayBuilder().data(data);
  }

  static ItemDisplayBuilder item(Item data) {
    Objects.requireNonNull(data);
    return new ItemDisplayBuilder().data(data);
  }

  static TextDisplayBuilder text(Component data) {
    Objects.requireNonNull(data);
    return new TextDisplayBuilder().data(data);
  }
}
