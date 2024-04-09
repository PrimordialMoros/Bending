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

package me.moros.bending.paper.platform.item;

import java.util.Optional;
import java.util.function.Supplier;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.paper.platform.BukkitPersistentDataHolder;
import me.moros.bending.paper.platform.PlatformAdapter;
import org.bukkit.inventory.ItemStack;

public final class BukkitItem implements ItemSnapshot {
  private final Item type;
  private final ItemStack handle;
  private final Supplier<DataHolder> holderSupplier;

  public BukkitItem(ItemStack handle) {
    this.type = PlatformAdapter.fromBukkitItem(handle.getType());
    this.handle = handle.clone();
    this.holderSupplier = Suppliers.lazy(() -> BukkitPersistentDataHolder.create(this.handle));
  }

  public ItemStack copy() {
    return handle.clone();
  }

  @Override
  public Item type() {
    return type;
  }

  @Override
  public int amount() {
    return handle.getAmount();
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return holderSupplier.get().get(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    holderSupplier.get().add(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    holderSupplier.get().remove(key);
  }
}
