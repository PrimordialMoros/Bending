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

package me.moros.bending.fabric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.fabric.mixin.accessor.EntityAccess;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.FabricBlockState;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.FabricEntity;
import me.moros.bending.platform.entity.FabricPlayer;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.world.FabricWorld;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class NativeAdapterImpl implements NativeAdapter {
  private final MinecraftServer server;
  private final FabricAudiences audiences;

  public NativeAdapterImpl(MinecraftServer server, FabricAudiences audiences) {
    this.server = server;
    this.audiences = audiences;
  }

  private ServerLevel adapt(World world) {
    return ((FabricWorld) world).handle();
  }

  private BlockState adapt(me.moros.bending.platform.block.BlockState state) {
    return ((FabricBlockState) state).handle();
  }

  private ServerPlayer adapt(Player player) {
    return ((FabricPlayer) player).handle();
  }

  private net.minecraft.world.entity.Entity adapt(Entity entity) {
    return ((FabricEntity) entity).handle();
  }

  private ItemStack adapt(Item item) {
    return BuiltInRegistries.ITEM.get(PlatformAdapter.rsl(item.key())).getDefaultInstance();
  }

  @Override
  public boolean setBlockFast(Block block, me.moros.bending.platform.block.BlockState state) {
    BlockPos position = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
    return adapt(block.world()).setBlock(position, adapt(state), 2);
  }

  @Override
  public boolean eyeInWater(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.WATER);
  }

  @Override
  public boolean eyeInLava(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.LAVA);
  }

  @Override
  public void sendNotification(Player player, Item item, Component title) {
    var conn = adapt(player).connection;
    conn.send(createNotification(item, title));
    conn.send(clearNotification());
  }

  @Override
  public int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    final int id = EntityAccess.idCounter().incrementAndGet();
    Collection<Packet<?>> packets = new ArrayList<>();
    packets.add(createEntity(id, center, EntityType.ARMOR_STAND, 0));
    if (!gravity) {
      packets.add(new EntityDataBuilder(id).invisible().marker().build());
    }
    if (velocity.lengthSq() > 0) {
      packets.add(addVelocity(id, velocity));
    }
    packets.add(setupEquipment(id, item));
    broadcast(packets, world, center);
    return id;
  }

  @Override
  public int createFallingBlock(World world, Position center, me.moros.bending.platform.block.BlockState data, Vector3d velocity, boolean gravity) {
    final int id = EntityAccess.idCounter().incrementAndGet();
    Collection<Packet<?>> packets = new ArrayList<>();
    final int blockDataId = net.minecraft.world.level.block.Block.getId(adapt(data));
    packets.add(createEntity(id, center, EntityType.FALLING_BLOCK, blockDataId));
    if (!gravity) {
      packets.add(new EntityDataBuilder(id).noGravity().build());
    }
    if (velocity.lengthSq() > 0) {
      packets.add(addVelocity(id, velocity));
    }
    broadcast(packets, world, center);
    return id;
  }

  @Override
  public void fakeBlock(Block block, me.moros.bending.platform.block.BlockState data) {
    broadcast(List.of(fakeBlockPacket(block, adapt(data))), block.world(), block, block.world().viewDistance() << 4);
  }

  @Override
  public void fakeBreak(Block block, byte progress) {
    broadcast(List.of(fakeBreakPacket(block, progress)), block.world(), block, block.world().viewDistance() << 4);
  }

  @Override
  public void destroy(int[] ids) {
    var packet = new ClientboundRemoveEntitiesPacket(ids);
    var playerList = server.getPlayerList();
    playerList.getPlayers().forEach(p -> p.connection.send(packet));
  }

  @Override
  public boolean tryPowerLightningRod(Block block) {
    ServerLevel level = adapt(block.world());
    BlockState data = level.getBlockState(new BlockPos(block.blockX(), block.blockY(), block.blockZ()));
    if (data.is(Blocks.LIGHTNING_ROD)) {
      BlockPos pos = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
      ((LightningRodBlock) data.getBlock()).onLightningStrike(data, adapt(block.world()), pos);
      return true;
    }
    return false;
  }

  private void broadcast(Collection<Packet<?>> packets, World world, Position center) {
    broadcast(packets, world, center, world.viewDistance() << 4);
  }

  private void broadcast(Collection<Packet<?>> packets, World world, Position center, int dist) {
    if (packets.isEmpty()) {
      return;
    }
    int distanceSq = dist * dist;
    for (var player : adapt(world).players()) {
      if (Vector3d.of(player.getX(), player.getY(), player.getZ()).distanceSq(center) <= distanceSq) {
        for (var packet : packets) {
          player.connection.send(packet);
        }
      }
    }
  }

  private ClientboundUpdateAdvancementsPacket createNotification(Item item, Component title) {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    String criteriaId = "bending:criteria_progress";
    ItemStack icon = adapt(item);
    net.minecraft.network.chat.Component nmsTitle = audiences.toNative(title);
    net.minecraft.network.chat.Component nmsDesc = net.minecraft.network.chat.Component.empty();
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

  private ClientboundUpdateAdvancementsPacket clearNotification() {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    return new ClientboundUpdateAdvancementsPacket(false, List.of(), Set.of(id), Map.of());
  }

  private ClientboundAddEntityPacket createEntity(int id, Position center, EntityType<?> type, int data) {
    double x = center.x();
    double y = center.y();
    double z = center.z();
    return new ClientboundAddEntityPacket(id, UUID.randomUUID(), x, y, z, 0, 0, type, data, Vec3.ZERO, 0);
  }

  private ClientboundSetEntityMotionPacket addVelocity(int id, Vector3d vel) {
    return new ClientboundSetEntityMotionPacket(id, new Vec3(vel.x(), vel.y(), vel.z()));
  }

  private ClientboundBlockUpdatePacket fakeBlockPacket(Position b, BlockState state) {
    return new ClientboundBlockUpdatePacket(new BlockPos(b.x(), b.y(), b.z()), state);
  }

  private ClientboundBlockDestructionPacket fakeBreakPacket(Position b, byte progress) {
    int id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
    return new ClientboundBlockDestructionPacket(id, new BlockPos(b.x(), b.y(), b.z()), progress);
  }

  private static final class EntityDataBuilder {
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

  private ClientboundSetEquipmentPacket setupEquipment(int id, Item item) {
    return new ClientboundSetEquipmentPacket(id, List.of(new Pair<>(EquipmentSlot.HEAD, adapt(item))));
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
