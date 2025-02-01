/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.fabric.platform.item;

import java.util.Optional;
import java.util.function.Supplier;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.fabric.platform.FabricPersistentDataHolder;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.minecraft.world.item.ItemStack;

public final class FabricItem implements ItemSnapshot {
  private final Item type;
  private final ItemStack handle;
  private final Supplier<DataHolder> holderSupplier;
  private final int hashcode;

  public FabricItem(ItemStack handle) {
    this.type = PlatformAdapter.fromFabricItem(handle.getItem());
    this.handle = handle.copy();
    this.holderSupplier = Suppliers.lazy(() -> FabricPersistentDataHolder.create(this.handle));
    this.hashcode = ItemStack.hashItemAndComponents(this.handle);
  }

  public ItemStack copy() {
    return handle.copy();
  }

  @Override
  public Item type() {
    return type;
  }

  @Override
  public int amount() {
    return handle.getCount();
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return holderSupplier.get().get(key);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return ItemStack.isSameItemSameComponents(handle, ((FabricItem) obj).handle);
  }

  @Override
  public int hashCode() {
    return hashcode;
  }
}
