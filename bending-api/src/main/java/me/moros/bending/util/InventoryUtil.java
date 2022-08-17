/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.util;

import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Utility class to handle inventory modification.
 */
// TODO better item replacing
public final class InventoryUtil {
  private static final ItemStack EMPTY_BOTTLE = new ItemStack(Material.POTION, 1);
  private static final ItemStack WATER_BOTTLE = new ItemStack(Material.POTION, 1);

  static {
    PotionMeta potionMeta = (PotionMeta) WATER_BOTTLE.getItemMeta();
    potionMeta.setBasePotionData(new PotionData(PotionType.WATER, false, false));
    WATER_BOTTLE.setItemMeta(potionMeta);
  }

  private InventoryUtil() {
  }

  /**
   * Check if a user has the specified item in their inventory.
   * @param user the user to check
   * @param itemStack the item and quantity of the item to check
   * @return true if the user has an inventory with the specified item, false otherwise
   */
  public static boolean hasItem(User user, ItemStack itemStack) {
    Inventory inventory = user.inventory();
    return inventory != null && inventory.containsAtLeast(itemStack, itemStack.getAmount());
  }

  /**
   * Check if a user has a water bottle in their inventory.
   * @param user the user to check
   * @return true if the user has an inventory with a water bottle, false otherwise
   * @see #hasItem(User, ItemStack)
   * @see #hasEmptyBottle(User)
   */
  public static boolean hasFullBottle(User user) {
    return hasItem(user, WATER_BOTTLE);
  }

  /**
   * Check if a user has an empty bottle in their inventory.
   * @param user the user to check
   * @return true if the user has an inventory with an empty bottle, false otherwise
   * @see #hasItem(User, ItemStack)
   * @see #hasFullBottle(User)
   */
  public static boolean hasEmptyBottle(User user) {
    return hasItem(user, EMPTY_BOTTLE);
  }

  /**
   * Try to remove an item from the user's inventory.
   * @param user the user to modify their inventory
   * @param itemStack the item to remove
   * @return true if the user had the specified item and it was successfully removed, false otherwise
   */
  public static boolean removeItem(User user, ItemStack itemStack) {
    if (!hasItem(user, itemStack)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItemAnySlot(itemStack).isEmpty();
  }

  /**
   * Try to fill an empty bottle by replacing it with a water bottle.
   * @param user the user to modify their inventory
   * @return true if the bottle was successfully filled, false otherwise
   * @see #emptyBottle(User)
   */
  public static boolean fillBottle(User user) {
    if (!hasEmptyBottle(user)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItemAnySlot(EMPTY_BOTTLE).isEmpty() && inventory.addItem(WATER_BOTTLE).isEmpty();
  }

  /**
   * Try to empty a water bottle by replacing it with an empty bottle.
   * @param user the user to modify their inventory
   * @return true if the bottle was successfully emptied, false otherwise
   * @see #fillBottle(User)
   */
  public static boolean emptyBottle(User user) {
    if (!hasFullBottle(user)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItemAnySlot(WATER_BOTTLE).isEmpty() && inventory.addItem(EMPTY_BOTTLE).isEmpty();
  }

  /**
   * Check if an entity is wearing metal armor.
   * @param entity the entity to check
   * @return true if the entity has an inventory and is wearing metal armor, false otherwise
   */
  public static boolean hasMetalArmor(LivingEntity entity) {
    EntityEquipment equipment = entity.getEquipment();
    if (equipment == null) {
      return false;
    }
    for (ItemStack item : equipment.getArmorContents()) {
      if (item != null && MaterialUtil.METAL_ARMOR.isTagged(item)) {
        return true;
      }
    }
    return false;
  }
}
