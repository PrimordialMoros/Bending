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

import java.util.ArrayList;
import java.util.List;

import me.moros.bending.platform.PlatformAdapter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitPlayerInventory extends BukkitInventory {
  private final PlayerInventory handle;

  public BukkitPlayerInventory(Player player) {
    super(player.getEquipment());
    this.handle = player.getInventory();
  }

  @Override
  public int selectedSlot() {
    return handle.getHeldItemSlot();
  }

  @Override
  public boolean has(Item type, int amount) {
    var mat = PlatformAdapter.toBukkitItemMaterial(type);
    return handle.contains(mat, amount);
  }

  @Override
  public int add(ItemSnapshot item) {
    var bukkitItem = PlatformAdapter.toBukkitItem(item);
    var result = handle.addItem(bukkitItem);
    return result.size();
  }

  @Override
  public boolean remove(Item type, int amount) {
    return removeItemAmount(PlatformAdapter.toBukkitItem(type), amount);
  }

  private boolean removeItemAmount(ItemStack toRemove, int amount) { // TODO redo logic, should only affect if enough items can be removed
    List<Integer> clearSlots = new ArrayList<>();
    ItemStack[] items = handle.getContents();

    for (int i = 0; i < items.length; i++) {
      ItemStack item = items[i];
      if (isEmpty(item)) {
        continue;
      }
      if (item.isSimilar(toRemove)) {
        if (item.getAmount() >= amount) {
          item.setAmount(item.getAmount() - amount);
          handle.setItem(i, item);
          for (int slot : clearSlots) {
            clearSlot(slot);
          }
          return true;
        } else {
          amount -= item.getAmount();
          clearSlots.add(i);
        }

        if (amount == 0) {
          for (int slot : clearSlots) {
            clearSlot(slot);
          }
          return true;
        }
      }
    }
    return false;
  }

  private boolean isEmpty(@Nullable ItemStack item) {
    return item == null || item.getType().isAir();
  }

  private void clearSlot(int slot) {
    ItemStack item = handle.getItem(slot);
    if (!isEmpty(item)) {
      item.setAmount(0);
      handle.setItem(slot, item);
    }
  }
}
