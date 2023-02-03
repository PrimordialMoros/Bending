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

package me.moros.bending.fabric.platform.item;

import java.util.Optional;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.world.item.ItemStack;

public class FabricItem implements ItemSnapshot {
  private final Item type;
  private final int amount;
  private final ItemStack handle;

  public FabricItem(ItemStack handle) {
    this.type = PlatformAdapter.fromFabricItem(handle.getItem());
    this.amount = handle.getCount();
    this.handle = handle.copy();
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
    return amount;
  }

  @Override
  public Optional<String> customName() {
    return customDisplayName().map(LegacyComponentSerializer.legacySection()::serialize);
  }

  @Override
  public Optional<Component> customDisplayName() {
    return handle().hasCustomHoverName() ? Optional.of(handle().getHoverName().asComponent()) : Optional.empty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return isValidKey(key) ? (Optional<T>) Optional.of(ItemUtil.hasKey(handle(), key)) : Optional.empty();
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    if (isValidKey(key) && value instanceof Boolean booleanValue) {
      ItemUtil.addKey(handle(), key, booleanValue);
    }
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    if (isValidKey(key)) {
      ItemUtil.removeKey(handle(), key);
    }
  }

  private static boolean isValidKey(DataKey<?> key) {
    return Metadata.isPersistent(key) && key.type() == Boolean.class;
  }
}
