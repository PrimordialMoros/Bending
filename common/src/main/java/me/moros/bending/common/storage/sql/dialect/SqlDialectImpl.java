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

package me.moros.bending.common.storage.sql.dialect;

import me.moros.storage.StorageType;

record SqlDialectImpl(StorageType type, boolean nativeUuidSupport) implements SqlDialect {
  @Override
  public boolean nativeUuid() {
    return switch (type()) {
      case POSTGRESQL, H2, HSQL -> true;
      case MARIADB -> nativeUuidSupport;
      default -> false;
    };
  }

  @Override
  public String extraTableOptions() {
    return switch (type()) {
      case MARIADB, MYSQL -> " DEFAULT CHARSET = utf8mb4";
      default -> "";
    };
  }

  @Override
  public String defineElementEnumType() {
    return switch (type()) {
      case POSTGRESQL, H2 -> "CREATE TYPE bending_element AS ENUM ('air', 'water', 'earth', 'fire');";
      default -> "";
    };
  }

  @Override
  public String elementEnumType() {
    return switch (type()) {
      case POSTGRESQL, H2 -> "bending_element";
      default -> "ENUM('air','water','earth','fire')";
    };
  }

  private boolean pgInsert() {
    return type() == StorageType.H2 || type() == StorageType.POSTGRESQL;
  }

  @Override
  public String insertAbilities() {
    String insert = "INSERT INTO bending_abilities (ability_id, ability_name) VALUES (?, ?) ON ";
    return insert + (pgInsert() ? "CONFLICT DO NOTHING" : "DUPLICATE KEY UPDATE ability_name = ability_name");
  }

  @Override
  public String insertUser() {
    return pgInsert() ?
      "MERGE INTO bending_users (user_id, board) VALUES (?, ?)" :
      "INSERT INTO bending_users (user_id, board) VALUES (?, ?) ON DUPLICATE KEY UPDATE board = VALUES (board)";
  }
}
