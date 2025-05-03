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

package me.moros.bending.common.storage.sql.dialect;

import java.sql.DatabaseMetaData;
import java.util.Locale;

import com.zaxxer.hikari.HikariDataSource;
import me.moros.bending.common.logging.Logger;
import me.moros.storage.StorageDataSource;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.flywaydb.core.api.MigrationVersion;

public sealed interface SqlDialect extends SqlQueries permits SqlDialectImpl {
  boolean nativeUuid();

  String extraTableOptions();

  default String uuidType() {
    return nativeUuid() ? "UUID" : "BINARY(16)";
  }

  String defineElementEnumType();

  String elementEnumType();

  String insertAbilities();

  String insertUser();

  static SqlDialect createFor(Logger logger, StorageDataSource source) {
    StorageType type = source.type();
    if (type == StorageType.SQLITE) {
      throw new IllegalArgumentException();
    }
    boolean nativeUuidSupport = true;
    if (type == StorageType.MYSQL || type == StorageType.MARIADB) {
      MigrationVersion version = mariaDBVersion(source.source());
      boolean isMariaDB = version != null;
      if (isMariaDB && type == StorageType.MYSQL) {
        logger.warn("Connected database is MariaDB but you've specified MySql engine in config.");
      }
      if (!(isMariaDB && version.isAtLeast("10.7"))) {
        logger.warn("You should consider upgrading your database to MariaDB 10.7+");
        nativeUuidSupport = false;
      }
    }
    return new SqlDialectImpl(type, nativeUuidSupport);
  }

  private static @Nullable MigrationVersion mariaDBVersion(HikariDataSource source) {
    try (var conn = source.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      String version = meta.getDatabaseProductVersion();
      if (version.toLowerCase(Locale.ROOT).contains("mariadb")) {
        String rawSemanticVersion = meta.getDatabaseMajorVersion() + "." + meta.getDatabaseMinorVersion();
        return MigrationVersion.fromVersion(rawSemanticVersion);
      }
    } catch (Exception ignore) {
    }
    return null;
  }
}
