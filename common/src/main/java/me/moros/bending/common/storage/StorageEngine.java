/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.storage;

import java.util.Optional;

import me.moros.bending.common.storage.file.loader.JsonLoader;
import me.moros.bending.common.storage.file.loader.Loader;
import me.moros.storage.StorageType;
import org.jspecify.annotations.Nullable;

public enum StorageEngine {
  // Remote databases
  MYSQL(StorageType.MYSQL),
  MARIADB(StorageType.MARIADB),
  POSTGRESQL(StorageType.POSTGRESQL),
  // Local databases
  H2(StorageType.H2),
  HSQL(StorageType.HSQL),
  // Flat file
  JSON("JSON", new JsonLoader());

  private final String name;
  private final StorageType type;
  private final Loader<?> loader;

  StorageEngine(StorageType type) {
    this(type.toString(), type, null);
  }

  StorageEngine(String name, Loader<?> loaderSupplier) {
    this(name, null, loaderSupplier);
  }

  StorageEngine(String name, @Nullable StorageType type, @Nullable Loader<?> loader) {
    this.name = name;
    this.type = type;
    this.loader = loader;
  }

  @Override
  public String toString() {
    return name;
  }

  public Optional<StorageType> type() {
    return Optional.ofNullable(type);
  }

  public Optional<Loader<?>> loader() {
    return Optional.ofNullable(loader);
  }
}
