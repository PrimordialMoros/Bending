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

package me.moros.bending.fabric.platform;

import java.util.Optional;

import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.common.util.DummyDataHolder;
import me.moros.bending.fabric.platform.item.ItemUtil;
import net.minecraft.world.item.ItemStack;

public record FabricPersistentDataHolder(ItemStack handle) implements DataHolder {
  public static DataHolder create(ItemStack itemStack) {
    return itemStack.isEmpty() ? DummyDataHolder.INSTANCE : new FabricPersistentDataHolder(itemStack);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return Metadata.isPersistent(key) ? Optional.ofNullable(ItemUtil.getKey(handle(), key)) : Optional.empty();
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    if (Metadata.isPersistent(key)) {
      ItemUtil.addKey(handle(), key, value);
    }
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    if (Metadata.isPersistent(key)) {
      ItemUtil.removeKey(handle(), key);
    }
  }
}
