/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.storage;

import java.io.File;
import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.storage.ConnectionBuilder;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Factory class that constructs and returns a Hikari-based database storage for Bending.
 * @see BendingStorage
 */
public final class StorageFactory {
  private StorageFactory() {
  }

  public static @Nullable BendingStorage createInstance(Logger logger, String dir) {
    Config config = ConfigManager.load(Config::new);
    if (config.engine == StorageType.SQLITE) {
      config.engine = StorageType.H2;
      logger.warn("Failed to parse engine type. Defaulting to H2.");
    }

    boolean h2 = config.engine == StorageType.H2;
    String path = h2 ? (dir + File.separator + "bending-h2;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE") : "";

    String poolName = config.engine.name() + " Bending Hikari Connection Pool";

    return ConnectionBuilder.create(StorageImpl::new, config.engine)
      .path(path).database(config.database).host(config.host).port(config.port)
      .username(config.username).password(config.password)
      .build(poolName, logger);
  }

  @ConfigSerializable
  private static final class Config extends Configurable {
    private StorageType engine = StorageType.H2;
    private String host = "localhost";
    private int port = 5432;
    private String username = "bending";
    private String password = "password";
    private String database = "bending";

    @Override
    public Iterable<String> path() {
      return List.of("storage");
    }
  }
}

