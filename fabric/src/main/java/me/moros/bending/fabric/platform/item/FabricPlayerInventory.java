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

package me.moros.bending.fabric.platform.item;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FabricPlayerInventory extends FabricInventory implements PlayerInventory {
  private final Inventory handle;

  public FabricPlayerInventory(ServerPlayer player) {
    super(player);
    this.handle = player.getInventory();
  }

  @Override
  public int selectedSlot() {
    return handle.selected;
  }

  @Override
  public boolean has(Item type, int amount) {
    if (amount <= 0) {
      return false;
    }
    ItemStack toMatch = new ItemStack(PlatformAdapter.toFabricItemType(type), amount);
    return hasItemInSubContainer(handle.items, toMatch)
      || hasItemInSubContainer(handle.armor, toMatch)
      || hasItemInSubContainer(handle.offhand, toMatch);
  }

  private boolean hasItemInSubContainer(Iterable<ItemStack> container, ItemStack neededItem) {
    for (ItemStack item : container) {
      if (!item.isEmpty() && ItemStack.isSameItem(item, neededItem)) {
        int count = item.getCount();
        if (count >= neededItem.getCount()) {
          return true;
        } else {
          neededItem.shrink(count);
        }
      }
    }
    return false;
  }

  @Override
  public void offer(ItemSnapshot item) {
    try (Transaction transaction = Transaction.openOuter()) {
      var transactionItem = ItemVariant.of(PlatformAdapter.toFabricItem(item));
      PlayerInventoryStorage.of(handle).offerOrDrop(transactionItem, item.amount(), transaction);
      transaction.commit();
    }
  }

  @Override
  public boolean remove(Item type, int amount) {
    try (Transaction transaction = Transaction.openOuter()) {
      var transactionItem = ItemVariant.of(PlatformAdapter.toFabricItemType(type));
      if (PlayerInventoryStorage.of(handle).extract(transactionItem, amount, transaction) == amount) {
        transaction.commit();
        return true;
      } else {
        transaction.abort();
      }
    }
    return false;
  }
}
