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

package me.moros.bending.sponge.platform.item;

import java.util.function.Supplier;

import me.moros.bending.api.platform.item.EquipmentSlot;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.sponge.platform.PlatformAdapter;
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

  protected ItemStack getOrAir(Supplier<? extends EquipmentType> type) {
    return handle().peek(type).orElseGet(AIR);
  }

  @Override
  public boolean canPlaceBlock() {
    return getOrAir(EquipmentTypes.MAIN_HAND).type().block().isPresent()
      || getOrAir(EquipmentTypes.OFF_HAND).type().block().isPresent();
  }

  @Override
  public ItemSnapshot item(EquipmentSlot slot) {
    return PlatformAdapter.fromSpongeItem(getOrAir(toSponge(slot)));
  }

  @Override
  public void item(EquipmentSlot slot, ItemSnapshot item) {
    handle().set(toSponge(slot), PlatformAdapter.toSpongeItem(item));
  }

  private static Supplier<? extends EquipmentType> toSponge(EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> EquipmentTypes.MAIN_HAND;
      case OFFHAND -> EquipmentTypes.OFF_HAND;
      case FEET -> EquipmentTypes.FEET;
      case LEGS -> EquipmentTypes.LEGS;
      case CHEST -> EquipmentTypes.CHEST;
      case HEAD -> EquipmentTypes.HEAD;
    };
  }
}
