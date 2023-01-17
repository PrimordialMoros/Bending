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

package me.moros.bending.adapter.impl.v1_19_R1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import io.papermc.paper.adventure.PaperAdventure;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.raytrace.BlockRayTrace;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.Context;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NativeAdapterImpl implements NativeAdapter {
  private final Function<me.moros.bending.platform.block.BlockState, BlockData> mapper;
  private final PlayerList playerList;

  public NativeAdapterImpl(Function<me.moros.bending.platform.block.BlockState, BlockData> mapper) {
    this.mapper = mapper;
    playerList = ((CraftServer) Bukkit.getServer()).getHandle();
  }

  private ServerLevel adapt(World world) {
    var bukkitWorld = Objects.requireNonNull(Bukkit.getWorld(world.name()));
    return ((CraftWorld) bukkitWorld).getHandle();
  }

  private BlockState adapt(me.moros.bending.platform.block.BlockState state) {
    return ((CraftBlockData) mapper.apply(state)).getState();
  }

  private ServerPlayer adapt(Player player) {
    return Objects.requireNonNull(playerList.getPlayer(player.uuid()));
  }

  private net.minecraft.world.entity.Entity adapt(Entity entity) {
    return Objects.requireNonNull(adapt(entity.world()).getEntity(entity.id()));
  }

  private ItemStack adapt(Item item) {
    return Registry.ITEM.get(new ResourceLocation(item.key().namespace(), item.key().value())).getDefaultInstance();
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
  public BlockRayTrace rayTraceBlocks(Context context, World world) {
    Vector3d s = context.origin();
    Vector3d e = context.endPoint();
    Vec3 startPos = new Vec3(s.x(), s.y(), s.z());
    Vec3 endPos = new Vec3(e.x(), e.y(), e.z());

    ClipContext.Block ccb = context.ignorePassable() ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE;
    Fluid ccf = context.ignoreLiquids() ? Fluid.NONE : Fluid.ANY;
    ClipContext clipContext = new ClipContext(startPos, endPos, ccb, ccf, null);

    return traverseBlocks(world, clipContext, context, CompositeRayTrace.miss(e));
  }

  private BlockRayTrace traverseBlocks(World world, ClipContext context, Context originalContext, BlockRayTrace miss) {
    Vec3 start = context.getFrom();
    Vec3 end = context.getTo();
    Level level = adapt(world);
    if (start.equals(end)) {
      return miss;
    } else {
      double d0 = Mth.lerp(-1.0E-7D, end.x, start.x);
      double d1 = Mth.lerp(-1.0E-7D, end.y, start.y);
      double d2 = Mth.lerp(-1.0E-7D, end.z, start.z);
      double d3 = Mth.lerp(-1.0E-7D, start.x, end.x);
      double d4 = Mth.lerp(-1.0E-7D, start.y, end.y);
      double d5 = Mth.lerp(-1.0E-7D, start.z, end.z);
      int i = Mth.floor(d3);
      int j = Mth.floor(d4);
      int k = Mth.floor(d5);
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(i, j, k);
      BlockRayTrace t0 = checkBlockCollision(level, context, originalContext, mutableBlockPos, miss, r -> hitFactory(world, r));
      if (t0 != null) {
        return t0;
      } else {
        double d6 = d0 - d3;
        double d7 = d1 - d4;
        double d8 = d2 - d5;
        int l = Mth.sign(d6);
        int i1 = Mth.sign(d7);
        int j1 = Mth.sign(d8);
        double d9 = l == 0 ? Double.MAX_VALUE : l / d6;
        double d10 = i1 == 0 ? Double.MAX_VALUE : i1 / d7;
        double d11 = j1 == 0 ? Double.MAX_VALUE : j1 / d8;
        double d12 = d9 * (l > 0 ? 1.0D - Mth.frac(d3) : Mth.frac(d3));
        double d13 = d10 * (i1 > 0 ? 1.0D - Mth.frac(d4) : Mth.frac(d4));
        double d14 = d11 * (j1 > 0 ? 1.0D - Mth.frac(d5) : Mth.frac(d5));
        BlockRayTrace result;
        do {
          if (d12 > 1.0D && d13 > 1.0D && d14 > 1.0D) {
            return miss;
          }
          if (d12 < d13) {
            if (d12 < d14) {
              i += l;
              d12 += d9;
            } else {
              k += j1;
              d14 += d11;
            }
          } else if (d13 < d14) {
            j += i1;
            d13 += d10;
          } else {
            k += j1;
            d14 += d11;
          }
          result = checkBlockCollision(level, context, originalContext, mutableBlockPos.set(i, j, k), miss, r -> hitFactory(world, r));
        } while (result == null);
        return result;
      }
    }
  }

  private @Nullable BlockRayTrace checkBlockCollision(Level world, ClipContext context, Context originalContext, BlockPos pos, BlockRayTrace miss, Function<BlockHitResult, BlockRayTrace> hitFactory) {
    if (originalContext.ignore(pos.getX(), pos.getY(), pos.getZ())) {
      return null;
    }
    BlockState iblockdata = world.getBlockStateIfLoaded(pos);
    if (iblockdata == null) {
      return miss;
    }
    if (iblockdata.isAir()) return null;
    FluidState fluid = iblockdata.getFluidState();
    Vec3 vec3d = context.getFrom();
    Vec3 vec3d1 = context.getTo();
    VoxelShape voxelshape = context.getBlockShape(iblockdata, world, pos);
    BlockHitResult res0 = world.clipWithInteractionOverride(vec3d, vec3d1, pos, voxelshape, iblockdata);
    VoxelShape voxelshape1 = context.getFluidShape(fluid, world, pos);
    BlockHitResult res1 = voxelshape1.clip(vec3d, vec3d1, pos);
    double d0 = res0 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res0.getLocation());
    double d1 = res1 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res1.getLocation());
    return d0 <= d1 ? hitFactory.apply(res0) : hitFactory.apply(res1);
  }

  private @Nullable BlockRayTrace hitFactory(World w, @Nullable BlockHitResult result) {
    if (result == null) {
      return null;
    }
    var l = result.getLocation();
    var p = result.getBlockPos();
    return CompositeRayTrace.hit(Vector3d.of(l.x(), l.y(), l.z()), new Block(w, p.getX(), p.getY(), p.getZ()));
  }

  private final int armorStandId = Registry.ENTITY_TYPE.getId(EntityType.ARMOR_STAND);

  @Override
  public void sendNotification(Player player, Item item, Component title) {
    var conn = adapt(player).connection;
    conn.send(createNotification(item, title));
    conn.send(clearNotification());
  }

  @Override
  public int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    final int id = net.minecraft.world.entity.Entity.nextEntityId();
    Collection<Packet<?>> packets = new ArrayList<>();
    packets.add(createMob(id, armorStandId, center));
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
    final int id = net.minecraft.world.entity.Entity.nextEntityId();
    Collection<Packet<?>> packets = new ArrayList<>();
    final int blockDataId = net.minecraft.world.level.block.Block.getId(adapt(data));
    packets.add(createFallingBlock(id, center, blockDataId));
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

  private ClientboundUpdateAdvancementsPacket clearNotification() {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    return new ClientboundUpdateAdvancementsPacket(false, List.of(), Set.of(id), Map.of());
  }

  private ClientboundAddEntityPacket createMob(int id, int entityTypeId, Position center) {
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

    // Data
    packetByteBuffer.writeVarInt(0);

    // Velocity
    packetByteBuffer.writeShort(0);
    packetByteBuffer.writeShort(0);
    packetByteBuffer.writeShort(0);

    return new ClientboundAddEntityPacket(packetByteBuffer);
  }

  private ClientboundAddEntityPacket createFallingBlock(int id, Position center, int blockId) {
    double x = center.x();
    double y = center.y();
    double z = center.z();
    return new ClientboundAddEntityPacket(id, UUID.randomUUID(), x, y, z, 0, 0, EntityType.FALLING_BLOCK, blockId, Vec3.ZERO, 0);
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
