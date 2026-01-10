/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.paper.platform;

import java.util.Optional;

import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.common.util.DummyDataHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;

public record BukkitPersistentDataHolder(PersistentDataHolder handle) implements DataHolder {
  public static DataHolder create(ItemStack itemStack) {
    if (!itemStack.isEmpty()) {
      var handle = itemStack.getItemMeta();
      if (handle != null) {
        return new BukkitPersistentDataHolder(handle);
      }
    }
    return DummyDataHolder.INSTANCE;
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    if (Metadata.isPersistent(key)) {
      var type = PlatformAdapter.dataType(key);
      if (type != null) {
        return Optional.ofNullable(handle().getPersistentDataContainer().get(PlatformAdapter.nsk(key), type));
      }
    }
    return Optional.empty();
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    if (Metadata.isPersistent(key)) {
      var type = PlatformAdapter.dataType(key);
      if (type != null) {
        handle().getPersistentDataContainer().set(PlatformAdapter.nsk(key), type, value);
      }
    }
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    if (Metadata.isPersistent(key)) {
      handle().getPersistentDataContainer().remove(PlatformAdapter.nsk(key));
    }
  }
}
