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

package me.moros.bending.fabric.platform.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.raytrace.CompositeRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.fabric.mixin.accessor.ChunkMapAccess;
import me.moros.bending.fabric.mixin.accessor.FallingBlockEntityAccess;
import me.moros.bending.fabric.platform.FabricMetadata;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.particle.ParticleMapper;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FabricWorld(ServerLevel handle) implements World {
  private net.minecraft.world.level.block.state.BlockState at(int x, int y, int z) {
    return handle().getBlockState(new BlockPos(x, y, z));
  }

  @Override
  public BlockType getBlockType(int x, int y, int z) {
    return getBlockState(x, y, z).type();
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return PlatformAdapter.fromFabricData(at(x, y, z));
  }

  @Override
  public AABB blockBounds(int x, int y, int z) {
    var b = at(x, y, z);
    if (PlatformAdapter.fromFabricData(b).type().isCollidable()) {
      var shape = b.getShape(handle(), new BlockPos(x, y, z));
      if (!shape.isEmpty()) {
        var box = shape.bounds();
        if (box.getSize() > 0) {
          Vector3d min = Vector3d.of(x + box.minX, y + box.minY, z + box.minZ);
          Vector3d max = Vector3d.of(x + box.maxX, y + box.maxY, z + box.maxZ);
          return new AABB(min, max);
        }
      }
    }
    return AABB.dummy();
  }

  @Override
  public DataHolder blockMetadata(int x, int y, int z) {
    return FabricMetadata.INSTANCE.metadata(key(), x, y, z);
  }

  private @Nullable BlockEntity blockEntity(int x, int y, int z) {
    return handle().getBlockEntity(new BlockPos(x, y, z));
  }

  @Override
  public boolean isTileEntity(Position position) {
    return blockEntity(position.blockX(), position.blockY(), position.blockZ()) != null;
  }

  @Override
  public @Nullable Lockable containerLock(Position position) {
    var tile = blockEntity(position.blockX(), position.blockY(), position.blockZ());
    return tile instanceof Lockable lockable ? lockable : null;
  }

  @Override
  public boolean setBlockState(int x, int y, int z, BlockState state) {
    return handle().setBlock(new BlockPos(x, y, z), PlatformAdapter.toFabricData(state), 2);
  }

  @Override
  public List<Entity> nearbyEntities(AABB box, Predicate<Entity> predicate, int limit) {
    net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(box.min.to(Vec3.class), box.max.to(Vec3.class));
    List<Entity> entities = new ArrayList<>();
    for (var vanillaEntity : handle().getEntities(null, aabb)) {
      Entity entity = PlatformAdapter.fromFabricEntity(vanillaEntity);
      if (predicate.test(entity)) {
        entities.add(entity);
        if (limit > 0 && entities.size() >= limit) {
          return entities;
        }
      }
    }
    return entities;
  }

  @Override
  public String name() {
    return ((ServerLevelData) handle().getLevelData()).getLevelName();
  }

  @Override
  public int minHeight() {
    return handle().getMinBuildHeight();
  }

  @Override
  public int maxHeight() {
    return handle().getMaxBuildHeight();
  }

  @Override
  public <T> void spawnParticle(ParticleContext<T> context) {
    var options = ParticleMapper.mapParticleOptions(context);
    if (options != null) {
      handle().sendParticles(options, context.position().x(), context.position().y(), context.position().z(),
        context.count(), context.offset().x(), context.offset().y(), context.offset().z(), context.extra()
      );
    }
  }

  @Override
  public CompositeRayTrace rayTraceEntities(Context context, double range) {
    Entity result = null;
    Vector3d resPos = null;
    double minDistSq = Double.MAX_VALUE;
    var dir = context.dir().normalize().multiply(range);
    var box = AABB.fromRay(context.origin(), dir, context.raySize());
    var aabb = new net.minecraft.world.phys.AABB(box.min.x(), box.min.y(), box.min.z(), box.max.x(), box.max.y(), box.max.z());
    var vec3d1 = new Vec3(context.origin().x(), context.origin().y(), context.origin().z());
    var vec3d2 = new Vec3(dir.x(), dir.y(), dir.z());
    for (var fabricEntity : handle().getEntities(null, aabb)) {
      var pos = fabricEntity.getBoundingBox().clip(vec3d1, vec3d2).orElse(null);
      if (pos != null) {
        Entity entity = PlatformAdapter.fromFabricEntity(fabricEntity);
        double distSq = pos.distanceToSqr(vec3d1);
        if (distSq < minDistSq) {
          result = entity;
          resPos = Vector3d.of(pos.x(), pos.y(), pos.z());
          minDistSq = distSq;
        }
      }
    }
    return result == null ? CompositeRayTrace.miss(context.endPoint()) : CompositeRayTrace.hit(resPos, result);
  }

  @Override
  public boolean isDay() {
    return dimension() == Dimension.OVERWORLD && handle().isDay();
  }

  @Override
  public boolean isNight() {
    return dimension() == Dimension.OVERWORLD && handle().isNight();
  }

  @Override
  public boolean breakNaturally(int x, int y, int z) {
    return handle().destroyBlock(new BlockPos(x, y, z), true);
  }

  @Override
  public Entity dropItem(Position position, ItemSnapshot item, boolean canPickup) {
    var type = PlatformAdapter.toFabricItem(item);
    ItemEntity droppedItem = new ItemEntity(handle(), position.x(), position.y(), position.z(), type);
    droppedItem.setNeverPickUp();
    handle().addFreshEntity(droppedItem);
    return PlatformAdapter.fromFabricEntity(droppedItem);
  }


  @Override
  public Entity createFallingBlock(Position center, BlockState state, boolean gravity) {
    var data = PlatformAdapter.toFabricData(state);
    var fabricEntity = FallingBlockEntityAccess.bending$create(handle(), center.x(), center.y(), center.z(), data);
    fabricEntity.time = 1; // Is this needed?
    fabricEntity.setNoGravity(!gravity);
    fabricEntity.dropItem = false;
    ((FallingBlockEntityAccess) fabricEntity).bending$cancelDrop(true);
    handle().addFreshEntity(fabricEntity);
    return PlatformAdapter.fromFabricEntity(fabricEntity);
  }

  @Override
  public Entity createArmorStand(Position center, Item type, boolean gravity) {
    var item = PlatformAdapter.toFabricItem(type);
    var fabricEntity = new ArmorStand(handle(), center.x(), center.y(), center.z());
    fabricEntity.setInvulnerable(true);
    fabricEntity.setInvisible(true);
    fabricEntity.setNoGravity(!gravity);
    fabricEntity.setItemSlot(EquipmentSlot.HEAD, item);
    handle().addFreshEntity(fabricEntity);
    return PlatformAdapter.fromFabricEntity(fabricEntity);
  }

  @Override
  public int lightLevel(int x, int y, int z) {
    return handle().getMaxLocalRawBrightness(new BlockPos(x, y, z));
  }

  @Override
  public Dimension dimension() {
    var t = handle().dimensionTypeId();
    if (t.equals(BuiltinDimensionTypes.OVERWORLD) || t.equals(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
      return Dimension.OVERWORLD;
    } else if (t.equals(BuiltinDimensionTypes.NETHER)) {
      return Dimension.NETHER;
    } else if (t.equals(BuiltinDimensionTypes.END)) {
      return Dimension.END;
    } else {
      return Dimension.CUSTOM;
    }
  }

  @Override
  public CompletableFuture<?> loadChunkAsync(int x, int z) {
    return handle().getChunkSource().getChunkFuture(x, z, ChunkStatus.EMPTY, false);
  }

  @Override
  public int viewDistance() {
    return ((ChunkMapAccess) handle().getChunkSource().chunkMap).viewDistance() - 1;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return handle().players();
  }

  @Override
  public @NonNull Key key() {
    return handle().dimension().location();
  }
}
