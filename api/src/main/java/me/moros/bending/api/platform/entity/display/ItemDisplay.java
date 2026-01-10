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

package me.moros.bending.api.platform.entity.display;

import me.moros.bending.api.platform.item.Item;

public sealed interface ItemDisplay extends Display<Item> permits ItemDisplayImpl {
  DisplayType displayType();

  @Override
  ItemDisplayBuilder toBuilder();

  enum DisplayType {
    NONE(0),
    THIRD_PERSON_LEFT_HAND(1),
    THIRD_PERSON_RIGHT_HAND(2),
    FIRST_PERSON_LEFT_HAND(3),
    FIRST_PERSON_RIGHT_HAND(4),
    HEAD(5),
    GUI(6),
    GROUND(7),
    FIXED(8);

    private final byte id;

    DisplayType(int index) {
      this.id = (byte) index;
    }

    public byte getId() {
      return this.id;
    }
  }
}
