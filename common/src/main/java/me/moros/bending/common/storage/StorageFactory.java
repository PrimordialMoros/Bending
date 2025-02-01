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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.common.Bending;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.storage.file.loader.Loader;
import me.moros.storage.Builder;
import me.moros.storage.StorageDataSource;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/**
 * Factory class that constructs and returns a Hikari-based database storage for Bending.
 * @see BendingStorage
 */
public record StorageFactory(Bending plugin) {
  public @Nullable BendingStorage createInstance() {
    Config config = ConfigManager.load(Config::new);
    return config.engine.loader().map(this::fileStorage).orElseGet(() -> sqlStorage(config));
  }

  private BendingStorage fileStorage(Loader<?> loader) {
    return new FileStorage(plugin.logger(), plugin.path().resolve("data").resolve("flatfile"), loader);
  }

  private @Nullable BendingStorage sqlStorage(Config config) {
    StorageType storageType = config.engine.type().orElseThrow();
    Builder builder = StorageDataSource.builder(storageType).database(config.database)
      .host(config.host).port(config.port).username(config.username).password(config.password);
    builder.configure(c -> {
      c.setMaximumPoolSize(config.poolSettings.maximumPoolSize);
      c.setMinimumIdle(config.poolSettings.minimumIdle);
      c.setMaxLifetime(config.poolSettings.maxLifetime);
      c.setKeepaliveTime(config.poolSettings.keepAliveTime);
      c.setConnectionTimeout(config.poolSettings.connectionTimeout);
    });
    if (storageType.isLocal()) {
      switch (storageType) {
        case HSQL -> builder.properties(p -> {
          p.put("sql.syntax_pgs", true);
          p.put("hsqldb.default_table_type", "cached");
        });
        case H2 -> builder.properties(p -> {
          p.put("MODE", "PostgreSQL");
          p.put("DB_CLOSE_ON_EXIT", false);
        });
      }
      Path parent = plugin.path().resolve("data").resolve(storageType.realName());
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        plugin.logger().error(e.getMessage(), e);
        return null;
      }
      // Convert to uri and back to path - needed only for windows compatibility
      builder.path(Path.of(parent.resolve("bending").toUri()));
    }
    StorageDataSource data = builder.build("bending-hikari");
    return data == null ? null : new SqlStorage(plugin.logger(), data);
  }

  private static final class Config implements Configurable {
    @Comment("""
      Available options:
      - Remote:
        > POSTGRESQL (preferred)
        > MARIADB
        > MYSQL
      - Local:
        > H2 (preferred)
        > HSQL
        > JSON""")
    private StorageEngine engine = StorageEngine.H2;
    private String host = "localhost";
    private int port = 5432;
    private String username = "bending";
    private String password = "password";
    private String database = "bending";
    private PoolSettings poolSettings = new PoolSettings();

    @Override
    public List<String> path() {
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

