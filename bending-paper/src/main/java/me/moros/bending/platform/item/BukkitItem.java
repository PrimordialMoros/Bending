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

import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BukkitItem implements ItemSnapshot {
  private final Item type;
  private final int amount;
  private Supplier<ItemMeta> handle;

  private BukkitItem(BukkitItem copy, int amount) {
    this.type = copy.type;
    this.amount = amount;
    this.handle = copy.handle;
  }

  public BukkitItem(ItemStack handle) {
    this.type = PlatformAdapter.ITEM_MATERIAL_INDEX.keyOrThrow(handle.getType());
    this.amount = handle.getAmount();
    this.handle = Suppliers.lazy(handle::getItemMeta);
  }

  public ItemMeta handle() {
    return handle.get();
  }

  @Override
  public boolean hasPersistentMetadata(Key key) {
    return handle().getPersistentDataContainer().has(PlatformAdapter.nsk(key));
  }

  @Override
  public boolean addPersistentMetadata(Key key) {
    var nsk = PlatformAdapter.nsk(key);
    if (handle().getPersistentDataContainer().has(nsk)) {
      return false;
    }
    handle().getPersistentDataContainer().set(nsk, PersistentDataType.BYTE, Metadata.EMPTY);
    return true;
  }

  @Override
  public void removePersistentMetadata(Key key) {
    handle().getPersistentDataContainer().remove(PlatformAdapter.nsk(key));
  }

  @Override
  public Item type() {
    return type;
  }

  @Override
  public int amount() {
    return amount;
  }

  @Override
  public Optional<String> customName() {
    return customDisplayName().map(LegacyComponentSerializer.legacySection()::serialize);
  }

  @Override
  public Optional<Component> customDisplayName() {
    return Optional.ofNullable(handle().displayName());
  }

  @Override
  public ItemSnapshot withAmount(int amount) {
    return amount == amount() ? this : new BukkitItem(this, amount);
  }
}
