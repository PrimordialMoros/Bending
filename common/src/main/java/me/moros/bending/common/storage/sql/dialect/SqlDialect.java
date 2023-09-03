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

package me.moros.bending.common.storage.sql.dialect;

import java.sql.DatabaseMetaData;
import java.util.Locale;

import com.zaxxer.hikari.HikariDataSource;
import me.moros.storage.StorageDataSource;
import me.moros.storage.StorageType;
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

  static SqlDialect createFor(StorageDataSource source) {
    StorageType type = source.type();
    if (type == StorageType.SQLITE) {
      throw new IllegalArgumentException();
    }
    if (type == StorageType.MYSQL || type == StorageType.MARIADB) {
      type = isMariaDb(source.source(), "10.7") ? StorageType.MARIADB : StorageType.MYSQL;
    }
    return new SqlDialectImpl(type);
  }

  private static boolean isMariaDb(HikariDataSource source, String minVersion) {
    try (var conn = source.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      String version = meta.getDatabaseProductVersion();
      if (version.toLowerCase(Locale.ROOT).contains("mariadb")) {
        String rawSemanticVersion = meta.getDatabaseMajorVersion() + "." + meta.getDatabaseMinorVersion();
        return MigrationVersion.fromVersion(rawSemanticVersion).isAtLeast(minVersion);
      }
    } catch (Exception ignore) {
    }
    return false;
  }
}
