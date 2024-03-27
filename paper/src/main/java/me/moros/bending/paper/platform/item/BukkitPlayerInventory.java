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

package me.moros.bending.paper.platform.item;

import java.util.Objects;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BukkitPlayerInventory extends BukkitInventory implements PlayerInventory {
  private final org.bukkit.inventory.PlayerInventory handle;

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
    return handle.contains(PlatformAdapter.toBukkitItemMaterial(type), amount);
  }

  @Override
  public void offer(ItemSnapshot item) {
    var bukkitItem = PlatformAdapter.toBukkitItem(item);
    int leftover = handle.addItem(bukkitItem).size();
    if (leftover > 0) {
      bukkitItem.setAmount(leftover);
      var player = Objects.requireNonNull(handle.getHolder());
      player.getWorld().dropItemNaturally(player.getEyeLocation().subtract(0, 0.3, 0), bukkitItem);
    }
  }

  @Override
  public boolean remove(Item type, int amount) {
    ItemStack bukkitItem = PlatformAdapter.toBukkitItem(type);
    bukkitItem.setAmount(amount);
    var toReturn = handle.removeItemAnySlot(bukkitItem).values().stream().findAny().orElse(null);
    if (toReturn != null) {
      handle.addItem(toReturn); // Add item back
      return false;
    }
    return true;
  }
}
