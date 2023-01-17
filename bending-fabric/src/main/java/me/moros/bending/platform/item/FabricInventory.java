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
  public int selectedSlot() {
    return -1;
  }

  @Override
  public void setItemInMainHand(ItemSnapshot snapshot) {
    handle.setItemInHand(InteractionHand.MAIN_HAND, PlatformAdapter.toFabricItem(snapshot));
  }

  @Override
  public boolean canPlaceBlock() {
    return handle.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BlockItem ||
      handle.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof BlockItem;
  }

  @Override
  public ItemSnapshot itemInMainHand() {
    return new FabricItem(handle.getItemInHand(InteractionHand.MAIN_HAND));
  }

  @Override
  public ItemSnapshot itemInOffHand() {
    return new FabricItem(handle.getItemInHand(InteractionHand.OFF_HAND));
  }

  @Override
  public boolean has(Item type, int amount) {
    return false;
  }

  @Override
  public int add(ItemSnapshot item) {
    return item.amount();
  }

  @Override
  public boolean remove(Item type, int amount) {
    return false;
  }

  @Override
  public ArmorContents<ItemSnapshot> armor() {
    ItemSnapshot h = PlatformAdapter.fromFabricItem(handle.getItemBySlot(EquipmentSlot.HEAD));
    ItemSnapshot c = PlatformAdapter.fromFabricItem(handle.getItemBySlot(EquipmentSlot.CHEST));
    ItemSnapshot l = PlatformAdapter.fromFabricItem(handle.getItemBySlot(EquipmentSlot.LEGS));
    ItemSnapshot b = PlatformAdapter.fromFabricItem(handle.getItemBySlot(EquipmentSlot.FEET));
    return ArmorContents.of(h, c, l, b);
  }

  @Override
  public void equipArmor(ArmorContents<ItemSnapshot> armor) {
    handle.setItemSlot(EquipmentSlot.HEAD, PlatformAdapter.toFabricItem(armor.helmet()));
    handle.setItemSlot(EquipmentSlot.CHEST, PlatformAdapter.toFabricItem(armor.chestplate()));
    handle.setItemSlot(EquipmentSlot.LEGS, PlatformAdapter.toFabricItem(armor.leggings()));
    handle.setItemSlot(EquipmentSlot.FEET, PlatformAdapter.toFabricItem(armor.boots()));
  }
}
