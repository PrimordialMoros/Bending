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

package me.moros.bending.api.platform.item;

public interface Inventory {
  int selectedSlot();

  boolean canPlaceBlock();

  ItemSnapshot itemInMainHand();

  void setItemInMainHand(ItemSnapshot item);

  ItemSnapshot itemInOffHand();

  default boolean has(Item type) {
    return has(type, 1);
  }

  boolean has(Item type, int amount);

  int add(ItemSnapshot item);

  default boolean remove(Item type) {
    return remove(type, 1);
  }

  boolean remove(Item type, int amount);

  ArmorContents<ItemSnapshot> armor();

  void equipArmor(ArmorContents<ItemSnapshot> armor);
}
