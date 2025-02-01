/*
 * Copyright 2020-2025 Moros
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

import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

public class BukkitInventory implements Inventory {
  private final EntityEquipment handle;

  public BukkitInventory(EntityEquipment handle) {
    this.handle = handle;
  }

  private EntityEquipment handle() {
    return handle;
  }

  @Override
  public boolean canPlaceBlock() {
    return handle().getItemInMainHand().getType().isBlock() || handle().getItemInOffHand().getType().isBlock();
  }

  @Override
  public ItemSnapshot item(me.moros.bending.api.platform.item.EquipmentSlot slot) {
    return PlatformAdapter.fromBukkitItem(handle().getItem(toBukkit(slot)));
  }

  @Override
  public void item(me.moros.bending.api.platform.item.EquipmentSlot slot, ItemSnapshot item) {
    handle().setItem(toBukkit(slot), PlatformAdapter.toBukkitItem(item));
  }

  private static EquipmentSlot toBukkit(me.moros.bending.api.platform.item.EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> EquipmentSlot.HAND;
      case OFFHAND -> EquipmentSlot.OFF_HAND;
      case FEET -> EquipmentSlot.FEET;
      case LEGS -> EquipmentSlot.LEGS;
      case CHEST -> EquipmentSlot.CHEST;
      case HEAD -> EquipmentSlot.HEAD;
    };
  }
}
