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

import java.util.Objects;

import me.moros.bending.api.platform.entity.display.ItemDisplay.DisplayType;
import me.moros.bending.api.platform.item.Item;

public final class ItemDisplayBuilder extends AbstractDisplayBuilder<Item, ItemDisplayBuilder> {
  private DisplayType displayType = DisplayType.NONE;

  ItemDisplayBuilder() {
    super(ItemDisplayImpl::new);
  }

  public ItemDisplayBuilder(ItemDisplay display) {
    super(ItemDisplayImpl::new, display);
    displayType(display.displayType());
  }

  public DisplayType displayType() {
    return displayType;
  }

  public ItemDisplayBuilder displayType(DisplayType displayType) {
    this.displayType = Objects.requireNonNull(displayType);
    return this;
  }
}
