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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.datafixers.util.Pair;
import me.moros.bending.api.adapter.PacketUtil;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractPacketUtil implements PacketUtil {
  private final PlayerList playerList;

  protected AbstractPacketUtil(PlayerList playerList) {
    this.playerList = playerList;
  }

  protected PlayerList playerList() {
    return playerList;
  }

  protected abstract ServerLevel adapt(World world);

  protected abstract BlockState adapt(me.moros.bending.api.platform.block.BlockState state);

  protected abstract net.minecraft.world.entity.Entity adapt(Entity entity);

  protected abstract ServerPlayer adapt(Player player);

  protected abstract ItemStack adapt(Item item);

  protected abstract net.minecraft.network.chat.Component adapt(Component component);

  protected abstract int nextEntityId();

  @Override
  public void sendNotification(Player player, Item item, Component title) {
    var conn = adapt(player).connection;
    conn.send(createNotification(item, title));
    conn.send(clearNotification());
  }

  @Override
  public int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    final int id = nextEntityId();
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
  public int createFallingBlock(World world, Position center, me.moros.bending.api.platform.block.BlockState data, Vector3d velocity, boolean gravity) {
    final int id = nextEntityId();
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
  public void fakeBlock(Block block, me.moros.bending.api.platform.block.BlockState data) {
    broadcast(List.of(fakeBlockPacket(block, adapt(data))), block.world(), block, block.world().viewDistance() << 4);
  }

  @Override
  public void fakeBreak(Block block, byte progress) {
    broadcast(List.of(fakeBreakPacket(block, progress)), block.world(), block, block.world().viewDistance() << 4);
  }

  @Override
  public void destroy(int[] ids) {
    var packet = new ClientboundRemoveEntitiesPacket(ids);
    playerList().getPlayers().forEach(p -> p.connection.send(packet));
  }

  protected void broadcast(Collection<Packet<?>> packets, World world, Position center) {
    broadcast(packets, world, center, world.viewDistance() << 4);
  }

  protected void broadcast(Collection<Packet<?>> packets, World world, Position center, int dist) {
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

  protected ClientboundUpdateAdvancementsPacket createNotification(Item item, Component title) {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    String criteriaId = "bending:criteria_progress";
    ItemStack icon = adapt(item);
    net.minecraft.network.chat.Component nmsTitle = adapt(title);
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

  protected ClientboundUpdateAdvancementsPacket clearNotification() {
    String identifier = "bending:notification";
    ResourceLocation id = new ResourceLocation(identifier);
    return new ClientboundUpdateAdvancementsPacket(false, List.of(), Set.of(id), Map.of());
  }

  protected ClientboundAddEntityPacket createEntity(int id, Position center, EntityType<?> type, int data) {
    double x = center.x();
    double y = center.y();
    double z = center.z();
    return new ClientboundAddEntityPacket(id, UUID.randomUUID(), x, y, z, 0, 0, type, data, Vec3.ZERO, 0);
  }

  protected ClientboundSetEntityMotionPacket addVelocity(int id, Vector3d vel) {
    return new ClientboundSetEntityMotionPacket(id, new Vec3(vel.x(), vel.y(), vel.z()));
  }

  protected ClientboundBlockUpdatePacket fakeBlockPacket(Position b, BlockState state) {
    return new ClientboundBlockUpdatePacket(new BlockPos(b.blockX(), b.blockY(), b.blockZ()), state);
  }

  protected ClientboundBlockDestructionPacket fakeBreakPacket(Position b, byte progress) {
    int id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
    return new ClientboundBlockDestructionPacket(id, new BlockPos(b.blockX(), b.blockY(), b.blockZ()), progress);
  }

  protected ClientboundSetEquipmentPacket setupEquipment(int id, Item item) {
    return new ClientboundSetEquipmentPacket(id, List.of(new Pair<>(EquipmentSlot.HEAD, adapt(item))));
  }
}
