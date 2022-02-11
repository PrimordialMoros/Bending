/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.util.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import me.moros.bending.model.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PacketUtil {
  private static final int armorStandId = Registry.ENTITY_TYPE.getId(EntityType.ARMOR_STAND);
  private static final int vexId = Registry.ENTITY_TYPE.getId(EntityType.VEX);

  private PacketUtil() {
  }

  public static void sendNotification(Player player, Material material, Component title) {
    Collection<Packet<?>> packets = List.of(createNotification(material, title), clearNotification());
    for (var packet : packets) {
      ((CraftPlayer) player).getHandle().connection.send(packet);
    }
  }

  public static int createArmorStand(World world, Vector3d center, Material material, Vector3d velocity, boolean gravity) {
    final int id = Entity.nextEntityId();
    Collection<Packet<?>> packets = new ArrayList<>();
    packets.add(createMob(id, armorStandId, center));
    if (!gravity) {
      packets.add(new EntityDataBuilder(id).invisible().marker().build());
    }
    if (velocity.lengthSq() > 0) {
      packets.add(addVelocity(id, velocity));
    }
    packets.add(setupEquipment(id, material));
    broadcast(packets, world, center);
    return id;
  }

  public static int createFallingBlock(World world, Vector3d center, BlockData data, Vector3d velocity, boolean gravity) {
    final int id = Entity.nextEntityId();
    Collection<Packet<?>> packets = new ArrayList<>();
    packets.add(createFallingBlock(id, center, Block.getId(((CraftBlockData) data).getState())));
    if (!gravity) {
      packets.add(new EntityDataBuilder(id).noGravity().build());
    }
    if (velocity.lengthSq() > 0) {
      packets.add(addVelocity(id, velocity));
    }
    broadcast(packets, world, center);
    return id;
  }

  public static void leash(World world, Vector3d center, int id1, int id2) {
    Collection<Packet<?>> packets = List.of(leash(id1, id2));
    broadcast(packets, world, center, (world.getViewDistance() + 1) << 4);
  }

  public static int createVex(World world, Vector3d center) {
    final int id = Entity.nextEntityId();
    Collection<Packet<?>> packets = new ArrayList<>();
    packets.add(createMob(id, vexId, center));
    packets.add(new EntityDataBuilder(id).noGravity().invisible().build());
    broadcast(packets, world, center);
    return id;
  }

  public static void teleportEntity(int id, World world, Vector3d center) {
    Collection<Packet<?>> packets = List.of(teleport(id, center));
    broadcast(packets, world, center);
  }

  public static void fakeBlock(World world, Vector3d center, BlockData data) {
    broadcast(List.of(fakeBlock(center, data)), world, center, world.getViewDistance() << 4);
  }

  public static void fakeBreak(World world, Vector3d center, byte progress) {
    broadcast(List.of(fakeBreak(center, progress)), world, center, world.getViewDistance() << 4);
  }

  public static void refreshBlocks(Collection<org.bukkit.block.Block> blocks, World world, Vector3d center) {
    broadcast(refreshBlocks(blocks), world, center, (world.getViewDistance() + 1) << 4);
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

  private static void broadcast(Collection<Packet<?>> packets, World world, Vector3d center) {
    broadcast(packets, world, center, world.getViewDistance() << 4);
  }

  private static void broadcast(Collection<Packet<?>> packets, World world, Vector3d center, int dist) {
    if (packets.isEmpty()) {
      return;
    }
    int distanceSq = dist * dist;
    for (var player : ((CraftWorld) world).getHandle().players()) {
      if (new Vector3d(player.getX(), player.getY(), player.getZ()).distanceSq(center) <= distanceSq) {
        for (var packet : packets) {
          player.connection.send(packet);
        }
      }
    }
  }

  private static ClientboundUpdateAdvancementsPacket createNotification(Material material, Component title) {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    String criteriaId = "bending:criteria_progress";
    net.minecraft.world.item.ItemStack icon = CraftItemStack.asNMSCopy(new ItemStack(material));
    net.minecraft.network.chat.Component nmsTitle = PaperAdventure.asVanilla(title);
    net.minecraft.network.chat.Component nmsDesc = PaperAdventure.asVanilla(Component.empty());
    FrameType type = FrameType.TASK;
    var advancement = Advancement.Builder.advancement()
      .display(icon, nmsTitle, nmsDesc, null, type, true, false, true)
      .addCriterion(criteriaId, new Criterion()).build(id);
    AdvancementProgress progress = new AdvancementProgress();
    progress.update(Map.of(criteriaId, new Criterion()), new String[][]{});
    progress.grantProgress(criteriaId);
    var progressMap = Map.of(id, progress);
    return new ClientboundUpdateAdvancementsPacket(false, List.of(advancement), Set.of(), progressMap);
  }

  private static ClientboundUpdateAdvancementsPacket clearNotification() {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    return new ClientboundUpdateAdvancementsPacket(false, List.of(), Set.of(id), Map.of());
  }

  private static ClientboundSetEntityLinkPacket leash(int id1, int id2) {
    PacketByteBuffer packetByteBuffer = PacketByteBuffer.get();

    packetByteBuffer.writeInt(id1);
    packetByteBuffer.writeInt(id2);

    return new ClientboundSetEntityLinkPacket(packetByteBuffer);
  }

  private static ClientboundTeleportEntityPacket teleport(int id, Vector3d center) {
    PacketByteBuffer packetByteBuffer = PacketByteBuffer.get();

    packetByteBuffer.writeVarInt(id);

    // Position
    packetByteBuffer.writeDouble(center.x());
    packetByteBuffer.writeDouble(center.y());
    packetByteBuffer.writeDouble(center.z());

    // Rotation
    packetByteBuffer.writeByte(0);
    packetByteBuffer.writeByte(0);

    // Grounded
    packetByteBuffer.writeBoolean(false);

    return new ClientboundTeleportEntityPacket(packetByteBuffer);
  }

  private static ClientboundAddMobPacket createMob(int id, int entityTypeId, Vector3d center) {
    PacketByteBuffer packetByteBuffer = PacketByteBuffer.get();

    packetByteBuffer.writeVarInt(id);
    packetByteBuffer.writeUUID(UUID.randomUUID());
    packetByteBuffer.writeVarInt(entityTypeId);

    // Position
    packetByteBuffer.writeDouble(center.x());
    packetByteBuffer.writeDouble(center.y());
    packetByteBuffer.writeDouble(center.z());

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

  private static ClientboundAddEntityPacket createFallingBlock(int id, Vector3d center, int blockId) {
    double x = center.x();
    double y = center.y();
    double z = center.z();
    return new ClientboundAddEntityPacket(id, UUID.randomUUID(), x, y, z, 0, 0, EntityType.FALLING_BLOCK, blockId, Vec3.ZERO);
  }

  private static ClientboundSetEntityMotionPacket addVelocity(int id, Vector3d vel) {
    return new ClientboundSetEntityMotionPacket(id, new Vec3(vel.x(), vel.y(), vel.z()));
  }

  private static ClientboundBlockUpdatePacket fakeBlock(Vector3d b, BlockData data) {
    return new ClientboundBlockUpdatePacket(new BlockPos(b.x(), b.y(), b.z()), ((CraftBlockData) data).getState());
  }

  private static ClientboundBlockDestructionPacket fakeBreak(Vector3d b, byte progress) {
    int id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
    return new ClientboundBlockDestructionPacket(id, new BlockPos(b.x(), b.y(), b.z()), progress);
  }

  private static Collection<Packet<?>> refreshBlocks(Collection<org.bukkit.block.Block> blocks) {
    int size = blocks.size();
    if (size == 0) {
      return List.of();
    } else if (size == 1) {
      var b = blocks.iterator().next();
      return List.of(new ClientboundBlockUpdatePacket(new BlockPos(b.getX(), b.getY(), b.getZ()), ((CraftBlock) b).getNMS()));
    } else {
      Map<SectionPos, Short2ObjectMap<BlockState>> sectionMap = new HashMap<>();
      for (org.bukkit.block.Block b : blocks) {
        BlockState state = ((CraftBlock) b).getNMS();
        BlockPos blockPos = new BlockPos(b.getX(), b.getY(), b.getZ());
        SectionPos sectionPos = SectionPos.of(blockPos);
        sectionMap.computeIfAbsent(sectionPos, key -> new Short2ObjectArrayMap<>())
          .put(SectionPos.sectionRelativePos(blockPos), state);
      }
      return sectionMap.entrySet().stream()
        .map(e -> new ClientboundSectionBlocksUpdatePacket(e.getKey(), e.getValue(), true))
        .collect(Collectors.toList());
    }
  }

  private static class EntityDataBuilder {
    private final PacketByteBuffer packetByteBuffer;

    private EntityDataBuilder(int id) {
      packetByteBuffer = new PacketByteBuffer();
      packetByteBuffer.writeVarInt(id);
    }

    private EntityDataBuilder noGravity() {
      packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.GRAVITY, true);
      return this;
    }

    private EntityDataBuilder invisible() {
      packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ENTITY_STATUS, (byte) 0x20); // Invisible
      return this;
    }

    private EntityDataBuilder marker() {
      packetByteBuffer.writeDataWatcherEntry(DataWatcherKey.ARMOR_STAND_STATUS, (byte) (0x02 | 0x08 | 0x10)); // no gravity, no base plate, marker
      return this;
    }

    private ClientboundSetEntityDataPacket build() {
      packetByteBuffer.writeDataWatcherEntriesEnd();
      return new ClientboundSetEntityDataPacket(packetByteBuffer);
    }
  }

  private static ClientboundSetEquipmentPacket setupEquipment(int id, Material material) {
    return new ClientboundSetEquipmentPacket(id, List.of(new Pair<>(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(material)))));
  }

  private static final class PacketByteBuffer extends FriendlyByteBuf {
    private static final int byteSerializerId = EntityDataSerializers.getSerializedId(EntityDataSerializers.BYTE);
    private static final int booleanSerializerId = EntityDataSerializers.getSerializedId(EntityDataSerializers.BOOLEAN);

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
      writeVarInt(byteSerializerId);
      EntityDataSerializers.BYTE.write(this, value);
    }

    private void writeDataWatcherEntry(DataWatcherKey key, boolean value) {
      writeByte(key.index);
      writeVarInt(booleanSerializerId);
      EntityDataSerializers.BOOLEAN.write(this, value);
    }

    private void writeDataWatcherEntriesEnd() {
      writeByte(0xFF);
    }
  }

  private enum DataWatcherKey {
    ENTITY_STATUS(0),
    GRAVITY(5),
    ARMOR_STAND_STATUS(15);

    private final int index;

    DataWatcherKey(int index) {
      this.index = index;
    }
  }
}
