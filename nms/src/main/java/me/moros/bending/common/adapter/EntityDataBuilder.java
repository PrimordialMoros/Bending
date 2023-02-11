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

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

public class EntityDataBuilder {
  private final PacketByteBuffer packetByteBuffer;

  public EntityDataBuilder(int id) {
    packetByteBuffer = new PacketByteBuffer();
    packetByteBuffer.writeVarInt(id);
  }

  public EntityDataBuilder noGravity() {
    packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.GRAVITY, true);
    return this;
  }

  public EntityDataBuilder invisible() {
    packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ENTITY_STATUS, (byte) 0x20); // Invisible
    return this;
  }

  public EntityDataBuilder marker() {
    packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ARMOR_STAND_STATUS, (byte) (0x02 | 0x08 | 0x10)); // no gravity, no base plate, marker
    return this;
  }

  public ClientboundSetEntityDataPacket build() {
    packetByteBuffer.writeDataWatcherEntriesEnd();
    return new ClientboundSetEntityDataPacket(packetByteBuffer);
  }
}
