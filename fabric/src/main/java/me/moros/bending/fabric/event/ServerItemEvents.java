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
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ServerItemEvents {
  private ServerItemEvents() {
  }

  public static final Event<DropItem> DROP_ITEM = EventFactory.createArrayBacked(DropItem.class, callbacks -> (entity, item) -> {
    for (var callback : callbacks) {
      if (!callback.onDrop(entity, item)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<AccessLock> ACCESS_LOCK = EventFactory.createArrayBacked(AccessLock.class, callbacks -> (player, lock, item) -> {
    for (var callback : callbacks) {
      var result = callback.onAccess(player, lock, item);
      if (result != TriState.DEFAULT) {
        return result;
      }
    }
    return TriState.DEFAULT;
  });

  @FunctionalInterface
  public interface DropItem {
    boolean onDrop(ServerPlayer player, ItemStack item);
  }

  @FunctionalInterface
  public interface AccessLock {
    TriState onAccess(Player player, String lock, ItemStack item);
  }
}
