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
import java.util.function.Supplier;

import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Suppliers;
import org.bukkit.Bukkit;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

public record BukkitDataHolder(Metadatable handle) implements DataHolder {
  private static final Supplier<Plugin> plugin = Suppliers.lazy(() -> Bukkit.getPluginManager().getPlugin("bending"));

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return handle().getMetadata(key.value()).stream().map(MetadataValue::value)
      .filter(key.type()::isInstance).map(key.type()::cast).findAny();
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
    handle().setMetadata(key.value(), new FixedMetadataValue(plugin.get(), value));
  }

  @Override
  public <T> void remove(DataKey<T> key) {
    handle().removeMetadata(key.value(), plugin.get());
  }
}
