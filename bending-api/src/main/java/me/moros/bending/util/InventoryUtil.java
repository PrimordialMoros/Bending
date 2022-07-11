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

  public static boolean hasItem(User user, ItemStack itemStack) {
    Inventory inventory = user.inventory();
    return inventory != null && inventory.containsAtLeast(itemStack, itemStack.getAmount());
  }

  public static boolean hasFullBottle(User user) {
    return hasItem(user, WATER_BOTTLE);
  }

  public static boolean hasEmptyBottle(User user) {
    return hasItem(user, EMPTY_BOTTLE);
  }

  public static boolean removeItem(User user, ItemStack itemStack) {
    if (!hasItem(user, itemStack)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItem(itemStack).isEmpty();
  }

  public static boolean fillBottle(User user) {
    if (!hasEmptyBottle(user)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItem(EMPTY_BOTTLE).isEmpty() && inventory.addItem(WATER_BOTTLE).isEmpty();
  }

  public static boolean emptyBottle(User user) {
    if (!hasFullBottle(user)) {
      return false;
    }
    Inventory inventory = user.inventory();
    return inventory != null && inventory.removeItem(WATER_BOTTLE).isEmpty() && inventory.addItem(EMPTY_BOTTLE).isEmpty();
  }

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
