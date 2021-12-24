/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.util.packet;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import me.moros.bending.model.math.Vector3d;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PacketUtil {
  private PacketUtil() {
  }

  public static int createArmorStand(World world, Vector3d center, Material material) {
    final int id = Entity.nextEntityId();
    var packets = List.of(
      createArmorStand(id, center),
      makeInvisible(id),
      setupEquipment(id, material)
    );
    broadcast(packets, world, center);
    return id;
  }

  public static void destroy(int id) {
    destroy(new int[]{id});
  }

  public static void destroy(int[] ids) {
    var packet = new ClientboundRemoveEntitiesPacket(ids);
    for (Player player : Bukkit.getOnlinePlayers()) {
      ((CraftPlayer) player).getHandle().connection.send(packet);
    }
  }

  private static void broadcast(Collection<Packet<ClientGamePacketListener>> packets, World world, Vector3d center) {
    int distanceSq = 32 * 32;
    Location origin = center.toLocation(world);
    Location loc = origin.clone();
    for (Player player : world.getPlayers()) {
      if (player.getLocation(loc).distanceSquared(origin) <= distanceSq) {
        for (var packet : packets) {
          ((CraftPlayer) player).getHandle().connection.send(packet);
        }
      }
    }
  }

  private static ClientboundAddMobPacket createArmorStand(int id, Vector3d center) {
    PacketByteBuffer packetByteBuffer = PacketByteBuffer.get();

    packetByteBuffer.writeVarInt(id);
    packetByteBuffer.writeUUID(UUID.randomUUID());
    packetByteBuffer.writeVarInt(1);

    // Position
    packetByteBuffer.writeDouble(center.getX());
    packetByteBuffer.writeDouble(center.getY());
    packetByteBuffer.writeDouble(center.getZ());

    // Rotation
    packetByteBuffer.writeByte(0);
    packetByteBuffer.writeByte(0);

    // Head rotation
    packetByteBuffer.writeByte(0);

    // Velocity
    packetByteBuffer.writeShort(0);
    packetByteBuffer.writeShort(0);
    packetByteBuffer.writeShort(0);

    return new ClientboundAddMobPacket(packetByteBuffer);
  }

  private static ClientboundSetEntityDataPacket makeInvisible(int id) {
    PacketByteBuffer packetByteBuffer = PacketByteBuffer.get();
    packetByteBuffer.writeVarInt(id);
    packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ENTITY_STATUS, (byte) 0x20); // Invisible
    packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ARMOR_STAND_STATUS, (byte) (0x02 | 0x08 | 0x10)); // no gravity, no base plate, marker
    packetByteBuffer.writeDataWatcherEntriesEnd();
    return new ClientboundSetEntityDataPacket(packetByteBuffer);
  }


  private static ClientboundSetEquipmentPacket setupEquipment(int id, Material material) {
    return new ClientboundSetEquipmentPacket(id, List.of(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(material)))));
  }

  private static final class PacketByteBuffer extends FriendlyByteBuf {
    private static final int serializerTypeID = EntityDataSerializers.getSerializedId(EntityDataSerializers.BYTE);

    private static final PacketByteBuffer INSTANCE = new PacketByteBuffer();

    static PacketByteBuffer get() {
      INSTANCE.clear();
      return INSTANCE;
    }

    private PacketByteBuffer() {
      super(Unpooled.buffer());
    }

    private void writeDataWatcherEntry(DataWatcherKey key, byte value) {
      writeByte(key.index);
      writeVarInt(serializerTypeID);
      EntityDataSerializers.BYTE.write(this, value);
    }

    private void writeDataWatcherEntriesEnd() {
      writeByte(0xFF);
    }
  }

  private enum DataWatcherKey {
    ENTITY_STATUS(0),
    ARMOR_STAND_STATUS(15);

    private final int index;

    DataWatcherKey(int index) {
      this.index = index;
    }
  }
}
