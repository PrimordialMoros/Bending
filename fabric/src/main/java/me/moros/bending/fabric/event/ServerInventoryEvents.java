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

package me.moros.bending.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;

public final class ServerInventoryEvents {
  private ServerInventoryEvents() {
  }

  public static final Event<Hopper> HOPPER = EventFactory.createArrayBacked(Hopper.class, callbacks -> (container, itemEntity) -> {
    for (var callback : callbacks) {
      if (!callback.onItemPull(container, itemEntity)) {
        return false;
      }
    }
    return true;
  });

  @FunctionalInterface
  public interface Hopper {
    boolean onItemPull(Container container, ItemEntity itemEntity);
  }
}
