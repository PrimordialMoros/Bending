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

import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;

/**
 * Utility class to handle inventory modification.
 */
public final class InventoryUtil {
  private InventoryUtil() {
  }

  /**
   * Check if a user has a water bottle in their inventory.
   * @param user the user to check
   * @return true if the user has an inventory with a water bottle, false otherwise
   */
  public static boolean hasFullBottle(User user) {
    return user.inventory() instanceof PlayerInventory inv && inv.has(Item.POTION);
  }

  /**
   * Check if an entity is wearing metal armor.
   * @param entity the entity to check
   * @return true if the entity has an inventory and is wearing metal armor, false otherwise
   */
  public static boolean hasMetalArmor(LivingEntity entity) {
    Inventory inv = entity.inventory();
    if (inv != null) {
      for (var slot : EquipmentSlot.ARMOR) {
        if (MaterialUtil.METAL_ARMOR.isTagged(inv.item(slot))) {
          return true;
        }
      }
    }
    return false;
  }
}
