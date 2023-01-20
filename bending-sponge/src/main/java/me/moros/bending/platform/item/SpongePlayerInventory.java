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

package me.moros.bending.platform.item;

import me.moros.bending.platform.PlatformAdapter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryTypes;

public class SpongePlayerInventory extends SpongeInventory {
  private final PlayerInventory handle;

  public SpongePlayerInventory(ServerPlayer player) {
    super(player.inventory().equipment());
    this.handle = player.inventory();
  }

  @Override
  public int selectedSlot() {
    return handle.hotbar().selectedSlotIndex();
  }

  @Override
  public boolean has(Item type, int amount) {
    return handle.contains(ItemStack.of(PlatformAdapter.toSpongeItemType(type), amount));
  }

  @Override
  public int add(ItemSnapshot item) {
    var spongeItem = PlatformAdapter.toSpongeItem(item);
    var result = handle.offer(spongeItem);
    return result.rejectedItems().size();
  }

  @Override
  public boolean remove(Item type, int amount) {
    var queried = handle.query(QueryTypes.ITEM_TYPE.get().of(PlatformAdapter.toSpongeItemType(type)));
    return !queried.poll(amount).revertOnFailure();
  }
}
