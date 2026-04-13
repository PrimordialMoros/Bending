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

package me.moros.bending.fabric.platform.item;

import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;

public class FabricInventory implements Inventory {
  private final LivingEntity handle;

  public FabricInventory(LivingEntity handle) {
    this.handle = handle;
  }

  @Override
  public boolean canPlaceBlock() {
    return handle.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BlockItem ||
      handle.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof BlockItem;
  }

  @Override
  public ItemSnapshot item(me.moros.bending.api.platform.item.EquipmentSlot slot) {
    return PlatformAdapter.fromFabricItem(handle.getItemBySlot(toVanilla(slot)));
  }

  @Override
  public void item(me.moros.bending.api.platform.item.EquipmentSlot slot, ItemSnapshot snapshot) {
    handle.setItemSlot(toVanilla(slot), PlatformAdapter.toFabricItem(snapshot));
  }

  protected static EquipmentSlot toVanilla(me.moros.bending.api.platform.item.EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> EquipmentSlot.MAINHAND;
      case OFFHAND -> EquipmentSlot.OFFHAND;
      case FEET -> EquipmentSlot.FEET;
      case LEGS -> EquipmentSlot.LEGS;
      case CHEST -> EquipmentSlot.CHEST;
      case HEAD -> EquipmentSlot.HEAD;
    };
  }
}
