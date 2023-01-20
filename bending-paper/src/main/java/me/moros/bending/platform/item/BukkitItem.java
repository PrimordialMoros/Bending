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
import me.moros.bending.platform.BukkitDataHolder;
import me.moros.bending.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BukkitItem extends BukkitDataHolder implements ItemSnapshot {
  private final Item type;
  private final int amount;
  private Supplier<ItemMeta> handle;

  private BukkitItem(Item type, int amount, Supplier<ItemMeta> handle) {
    super(() -> null, handle);
    this.type = type;
    this.amount = amount;
    this.handle = handle;
  }

  public BukkitItem(ItemStack handle) {
    this(Item.registry().getOrThrow(PlatformAdapter.nsk(handle.getType().getKey())), handle.getAmount(), Suppliers.lazy(handle::getItemMeta));
  }

  public ItemMeta handle() {
    return handle.get();
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
}
