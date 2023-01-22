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

import java.util.Optional;
import java.util.function.Supplier;

import me.moros.bending.model.data.DataHolder;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.platform.BukkitDataHolder;
import me.moros.bending.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BukkitItem implements ItemSnapshot {
  private final ItemStack handle;
  private final Item type;
  private final Supplier<DataHolder> holder;

  public BukkitItem(ItemStack handle) {
    this.handle = handle.clone();
    this.type = Item.registry().getOrThrow(PlatformAdapter.nsk(this.handle.getType().getKey()));
    this.holder = Suppliers.lazy(() -> BukkitDataHolder.persistent(handle().getItemMeta()));
  }

  public ItemStack handle() {
    return handle;
  }

  @Override
  public Item type() {
    return type;
  }

  @Override
  public int amount() {
    return handle().getAmount();
  }

  @Override
  public Optional<String> customName() {
    return customDisplayName().map(LegacyComponentSerializer.legacySection()::serialize);
  }

  @Override
  public Optional<Component> customDisplayName() {
    return Optional.ofNullable(handle().getItemMeta()).map(ItemMeta::displayName);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return holder.get().get(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    holder.get().add(key, value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    holder.get().remove(key);
  }
}
