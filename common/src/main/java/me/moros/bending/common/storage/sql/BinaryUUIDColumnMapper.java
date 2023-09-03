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

package me.moros.bending.common.storage.sql;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

public final class BinaryUUIDColumnMapper implements ColumnMapper<UUID> {
  @Override
  public @Nullable UUID map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
    byte[] bytes = rs.getBytes(columnNumber);
    return bytes == null ? null : fromByteArray(bytes);
  }

  public static UUID fromByteArray(byte[] array) {
    ByteBuffer buffer = ByteBuffer.wrap(array);
    return new UUID(buffer.getLong(), buffer.getLong());
  }
}
