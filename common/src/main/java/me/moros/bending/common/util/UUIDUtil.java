/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UUIDUtil {
  private UUIDUtil() {
  }

  public static byte[] toBytes(UUID uuid) {
    return ByteBuffer.wrap(new byte[16])
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .array();
  }

  public static UUID fromBytes(byte[] array) {
    final ByteBuffer buffer = ByteBuffer.wrap(array);
    return new UUID(buffer.getLong(), buffer.getLong());
  }
}
