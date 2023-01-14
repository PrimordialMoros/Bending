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

import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.ItemStack;

public class SpongeItem implements ItemSnapshot {
  private final Item type;
  private final int amount;
  private final ItemStack handle;

  public SpongeItem(ItemStack handle) {
    this.type = PlatformAdapter.ITEM_MATERIAL_INDEX.valueOrThrow(handle.type());
    this.amount = handle.quantity();
    this.handle = handle;
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
    return handle().get(Keys.CUSTOM_NAME);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return handle().get(PlatformAdapter.dataKey(key));
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    handle().offer(PlatformAdapter.dataKey(key), value);
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    handle().remove(PlatformAdapter.dataKey(key));
  }
}
