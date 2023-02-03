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

package me.moros.bending.common.adapter;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializers;

public class PacketByteBuffer extends FriendlyByteBuf {
  private static final int byteSerializerId = EntityDataSerializers.getSerializedId(EntityDataSerializers.BYTE);
  private static final int booleanSerializerId = EntityDataSerializers.getSerializedId(EntityDataSerializers.BOOLEAN);

  public PacketByteBuffer() {
    super(Unpooled.buffer());
  }

  public void writeDataWatcherEntry(DataWatcherKey key, byte value) {
    writeByte(key.index());
    writeVarInt(byteSerializerId);
    EntityDataSerializers.BYTE.write(this, value);
  }

  public void writeDataWatcherEntry(DataWatcherKey key, boolean value) {
    writeByte(key.index());
    writeVarInt(booleanSerializerId);
    EntityDataSerializers.BOOLEAN.write(this, value);
  }

  public void writeDataWatcherEntriesEnd() {
    writeByte(0xFF);
  }
}
