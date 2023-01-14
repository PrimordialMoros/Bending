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

import java.util.function.Supplier;

import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.platform.PlatformAdapter;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentType;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;

public class SpongeInventory implements Inventory {
  private static final Supplier<ItemStack> AIR = Suppliers.lazy(() -> ItemStack.of(ItemTypes.AIR));

  private final EquipmentInventory handle;

  public SpongeInventory(EquipmentInventory handle) {
    this.handle = handle;
  }

  public EquipmentInventory handle() {
    return handle;
  }

  @Override
  public int selectedSlot() {
    return -1;
  }

  protected ItemStack getOrAir(Supplier<? extends EquipmentType> type) {
    return handle().peek(type).orElseGet(AIR);
  }

  @Override
  public void setItemInMainHand(ItemSnapshot snapshot) {
    handle().set(EquipmentTypes.MAIN_HAND, PlatformAdapter.toSpongeItem(snapshot));
  }

  @Override
  public boolean canPlaceBlock() {
    return getOrAir(EquipmentTypes.MAIN_HAND).type().block().isPresent() ||
      getOrAir(EquipmentTypes.OFF_HAND).type().block().isPresent();
  }

  @Override
  public ItemSnapshot itemInMainHand() {
    return new SpongeItem(getOrAir(EquipmentTypes.MAIN_HAND));
  }

  @Override
  public ItemSnapshot itemInOffHand() {
    return new SpongeItem(getOrAir(EquipmentTypes.OFF_HAND));
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
    ItemSnapshot h = PlatformAdapter.fromSpongeItem(getOrAir(EquipmentTypes.HEAD));
    ItemSnapshot c = PlatformAdapter.fromSpongeItem(getOrAir(EquipmentTypes.CHEST));
    ItemSnapshot l = PlatformAdapter.fromSpongeItem(getOrAir(EquipmentTypes.LEGS));
    ItemSnapshot b = PlatformAdapter.fromSpongeItem(getOrAir(EquipmentTypes.FEET));
    return ArmorContents.of(h, c, l, b);
  }

  @Override
  public void equipArmor(ArmorContents<ItemSnapshot> armor) {
    handle().set(EquipmentTypes.HEAD, PlatformAdapter.toSpongeItem(armor.helmet()));
    handle().set(EquipmentTypes.CHEST, PlatformAdapter.toSpongeItem(armor.chestplate()));
    handle().set(EquipmentTypes.LEGS, PlatformAdapter.toSpongeItem(armor.leggings()));
    handle().set(EquipmentTypes.FEET, PlatformAdapter.toSpongeItem(armor.boots()));
  }
}
