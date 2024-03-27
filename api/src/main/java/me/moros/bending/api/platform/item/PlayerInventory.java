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


/**
 * Represents a player inventory.
 * Supports transactional operations that revert changes if failed.
 */
public interface PlayerInventory extends Inventory {
  int selectedSlot();

  /**
   * Check if this inventory an item.
   * @param type the item type to check
   * @return true if this inventory contains at least 1 of the specified item type, false otherwise
   */
  default boolean has(Item type) {
    return has(type, 1);
  }

  /**
   * Check if this inventory an item.
   * @param type the item type to check
   * @param amount the minimum amount to check
   * @return true if this inventory contains at least the given amount of the specified item type, false otherwise
   */
  boolean has(Item type, int amount);

  /**
   * Offer an item to this inventory. Any excess amount will be dropped.
   * @param item the item to offer
   */
  void offer(ItemSnapshot item);

  /**
   * Remove a single item from this inventory.
   * @param type the type of item to remove
   * @return if the exact amount was removed, false otherwise
   */
  default boolean remove(Item type) {
    return remove(type, 1);
  }

  /**
   * Remove an item from this inventory.
   * @param type the type of item to remove
   * @param amount the amount to remove
   * @return true if the exact amount was removed, false otherwise
   */
  boolean remove(Item type, int amount);
}
