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

public sealed interface TextDisplay extends Display<Component> permits TextDisplayImpl {
  int lineWidth();

  int backgroundColor();

  byte opacity();

  TextFlags textFlags();

  @Override
  TextDisplayBuilder toBuilder();

  enum Alignment {
    CENTER(0),
    LEFT(1),
    RIGHT(2);

    private final byte id;

    Alignment(int index) {
      this.id = (byte) index;
    }

    public byte getId() {
      return this.id;
    }
  }

  interface TextFlags {
    boolean hasShadow();

    boolean isSeeThrough();

    boolean hasDefaultBackground();

    Alignment alignment();
  }
}
