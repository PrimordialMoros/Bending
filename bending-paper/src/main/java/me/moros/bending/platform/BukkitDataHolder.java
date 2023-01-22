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

package me.moros.bending.platform;

import java.util.Optional;

import me.moros.bending.Bending;
import me.moros.bending.model.data.DataHolder;
import me.moros.bending.model.data.DataKey;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.persistence.PersistentDataHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BukkitDataHolder(@Nullable Metadatable handle,
                               @Nullable PersistentDataHolder persistentHandle) implements DataHolder {
  public static DataHolder nonPersistent(Metadatable handle) {
    return new BukkitDataHolder(handle, null);
  }

  public static DataHolder persistent(PersistentDataHolder persistentHandle) {
    return new BukkitDataHolder(null, persistentHandle);
  }

  public static <T extends Metadatable & PersistentDataHolder> DataHolder combined(T handle) {
    return new BukkitDataHolder(handle, handle);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return Metadata.isPersistent(key) ? getPersistent(key) : getMetadata(key);
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    if (Metadata.isPersistent(key)) {
      addPersistent(key, value);
    } else {
      addMetadata(key, value);
    }
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    if (Metadata.isPersistent(key)) {
      removePersistent(key);
    } else {
      removeMetadata(key);
    }
  }

  private <T> Optional<T> getMetadata(DataKey<T> key) {
    if (handle() == null) {
      return Optional.empty();
    }
    return handle().getMetadata(key.value()).stream().map(MetadataValue::value)
      .filter(key.type()::isInstance).map(key.type()::cast).findAny();
  }

  private <T> void addMetadata(DataKey<T> key, T value) {
    if (handle() != null) {
      handle().setMetadata(key.value(), new FixedMetadataValue(Bending.plugin(), value));
    }
  }

  private <T> void removeMetadata(DataKey<T> key) {
    if (handle() != null) {
      handle().removeMetadata(key.value(), Bending.plugin());
    }
  }

  private <T> Optional<T> getPersistent(DataKey<T> key) {
    if (persistentHandle() != null) {
      var type = PlatformAdapter.dataType(key);
      if (type != null) {
        return Optional.ofNullable(persistentHandle().getPersistentDataContainer().get(PlatformAdapter.nsk(key), type));
      }
    }
    return Optional.empty();
  }

  private <T> void addPersistent(DataKey<T> key, T value) {
    if (persistentHandle() != null) {
      var store = persistentHandle().getPersistentDataContainer();
      var nsk = PlatformAdapter.nsk(key);
      var type = PlatformAdapter.dataType(key);
      if (type != null && !store.has(nsk)) {
        store.set(nsk, type, value);
      }
    }
  }

  private <T> void removePersistent(DataKey<T> key) {
    if (persistentHandle() != null) {
      persistentHandle().getPersistentDataContainer().remove(PlatformAdapter.nsk(key));
    }
  }
}
