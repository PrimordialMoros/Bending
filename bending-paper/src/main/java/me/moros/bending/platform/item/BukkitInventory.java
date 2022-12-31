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
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class BukkitInventory implements Inventory {
  private final EntityEquipment handle;

  public BukkitInventory(EntityEquipment handle) {
    this.handle = handle;
  }

  public EntityEquipment handle() {
    return handle;
  }

  @Override
  public int selectedSlot() {
    return -1;
  }

  @Override
  public void renameMainHandItem(Component name) {
    handle().getItemInMainHand().editMeta(m -> m.displayName(name));
  }

  @Override
  public boolean canPlaceBlock() {
    return handle().getItemInMainHand().getType().isBlock() || handle().getItemInOffHand().getType().isBlock();
  }

  @Override
  public ItemSnapshot itemInMainHand() {
    return new BukkitItem(handle().getItemInMainHand());
  }

  @Override
  public ItemSnapshot itemInOffHand() {
    return new BukkitItem(handle().getItemInOffHand());
  }

  @Override
  public boolean has(Item type, int amount) {
    return false;
  }

  @Override
  public boolean remove(Item type, int amount) {
    return false;
  }

  @Override
  public int add(ItemSnapshot item) {
    return item.amount();
  }

  @Override
  public ArmorContents<ItemSnapshot> armor() {
    ItemSnapshot h = PlatformAdapter.fromBukkitItem(handle().getHelmet(), ItemSnapshot.AIR.get());
    ItemSnapshot c = PlatformAdapter.fromBukkitItem(handle().getChestplate(), ItemSnapshot.AIR.get());
    ItemSnapshot l = PlatformAdapter.fromBukkitItem(handle().getLeggings(), ItemSnapshot.AIR.get());
    ItemSnapshot b = PlatformAdapter.fromBukkitItem(handle().getBoots(), ItemSnapshot.AIR.get());
    return ArmorContents.of(h, c, l, b);
  }

  @Override
  public void equipArmor(ArmorContents<ItemSnapshot> armor) {
    var a = armor.map(PlatformAdapter::toBukkitItem);
    handle.setArmorContents(new ItemStack[]{a.boots(), a.leggings(), a.chestplate(), a.helmet()});
  }
}
