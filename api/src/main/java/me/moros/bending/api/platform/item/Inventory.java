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

package me.moros.bending.api.platform.item;

public interface Inventory {
  int selectedSlot();

  boolean canPlaceBlock();

  ItemSnapshot item(EquipmentSlot slot);

  void item(EquipmentSlot slot, ItemSnapshot item);

  default ItemSnapshot itemInMainHand() {
    return item(EquipmentSlot.MAINHAND);
  }

  default void setItemInMainHand(ItemSnapshot item) {
    item(EquipmentSlot.MAINHAND, item);
  }

  default ItemSnapshot itemInOffHand() {
    return item(EquipmentSlot.OFFHAND);
  }

  default void setItemInOffHand(ItemSnapshot item) {
    item(EquipmentSlot.OFFHAND, item);
  }

  default boolean has(Item type) {
    return has(type, 1);
  }

  boolean has(Item type, int amount);

  int add(ItemSnapshot item);

  default boolean remove(Item type) {
    return remove(type, 1);
  }

  boolean remove(Item type, int amount);

  @Deprecated(forRemoval = true)
  default ArmorContents<ItemSnapshot> armor() {
    ItemSnapshot h = item(EquipmentSlot.HEAD);
    ItemSnapshot c = item(EquipmentSlot.CHEST);
    ItemSnapshot l = item(EquipmentSlot.LEGS);
    ItemSnapshot b = item(EquipmentSlot.FEET);
    return ArmorContents.of(h, c, l, b);
  }

  @Deprecated(forRemoval = true)
  default void equipArmor(ArmorContents<ItemSnapshot> armor) {
    item(EquipmentSlot.HEAD, armor.helmet());
    item(EquipmentSlot.CHEST, armor.chestplate());
    item(EquipmentSlot.LEGS, armor.leggings());
    item(EquipmentSlot.FEET, armor.boots());
  }
}
