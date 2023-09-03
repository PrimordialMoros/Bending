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

package me.moros.bending.common.storage.sql.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__Rename_legacy_tables extends BaseJavaMigration {
  private static final String RENAME_TABLE = "ALTER TABLE %s RENAME TO %s;";

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    Set<String> tables = fetchTables(conn);
    List<String> queries = new ArrayList<>();
    renameTable(tables, "bending_abilities").ifPresent(queries::add);
    renameTable(tables, "bending_presets").ifPresent(queries::add);
    if (!queries.isEmpty()) {
      try (var statement = conn.createStatement()) {
        for (var query : queries) {
          statement.addBatch(query);
        }
        statement.executeBatch();
      }
    }
  }

  private Optional<String> renameTable(Set<String> tables, String table) {
    return tables.contains(table) ? Optional.of(RENAME_TABLE.formatted(table, table + "_old")) : Optional.empty();
  }

  private Set<String> fetchTables(Connection connection) throws Exception {
    Set<String> tables = new HashSet<>();
    try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, "%", null)) {
      while (rs.next()) {
        String tableName = rs.getString(3).toLowerCase(Locale.ROOT);
        if (tableName.startsWith("bending")) {
          tables.add(tableName);
        }
      }
    }
    return tables;
  }
}
