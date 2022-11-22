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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.storage.Builder;
import me.moros.storage.StorageDataSource;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Factory class that constructs and returns a Hikari-based database storage for Bending.
 * @see BendingStorage
 */
public final class StorageFactory {
  private StorageFactory() {
  }

  public static @Nullable BendingStorage createInstance(Bending plugin) {
    Config config = ConfigManager.load(Config::new);
    Builder builder = StorageDataSource.builder(config.engine).database(config.database)
      .host(config.host).port(config.port).username(config.username).password(config.password);
    builder.configure(c -> {
      c.setMaximumPoolSize(config.poolSettings.maximumPoolSize);
      c.setMinimumIdle(config.poolSettings.minimumIdle);
      c.setMaxLifetime(config.poolSettings.maxLifetime);
      c.setKeepaliveTime(config.poolSettings.keepAliveTime);
      c.setConnectionTimeout(config.poolSettings.connectionTimeout);
    });
    if (config.engine.isLocal()) {
      switch (config.engine) {
        case HSQL -> builder.properties(p -> {
          p.put("sql.syntax_mys", true);
          p.put("hsqldb.default_table_type", "cached");
        });
        case H2 -> builder.properties(p -> {
          p.put("MODE", "PostgreSQL");
          p.put("DB_CLOSE_ON_EXIT", false);
        });
      }
      Path parent = Path.of(plugin.getDataFolder().toString(), "data", config.engine.realName());
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        plugin.logger().error(e.getMessage(), e);
        return null;
      }
      builder.path("./" + parent.resolve("bending") + (config.engine == StorageType.SQLITE ? ".db" : ""));
    }
    StorageDataSource data = builder.build("bending-hikari", plugin.logger());
    if (data != null) {
      StorageImpl storage = new StorageImpl(data);
      if (storage.init(plugin::getResource)) {
        return storage;
      }
    }
    return null;
  }

  @ConfigSerializable
  private static final class Config extends Configurable {
    private StorageType engine = StorageType.H2;
    private String host = "localhost";
    private int port = 5432;
    private String username = "bending";
    private String password = "password";
    private String database = "bending";
    private PoolSettings poolSettings = new PoolSettings();

    @Override
    public Iterable<String> path() {
      return List.of("storage");
    }
  }

  @ConfigSerializable
  private static final class PoolSettings {
    private int maximumPoolSize = 6;
    private int minimumIdle = 6;
    private int maxLifetime = 1_800_000;
    private int keepAliveTime = 0;
    private int connectionTimeout = 5000;
  }
}

