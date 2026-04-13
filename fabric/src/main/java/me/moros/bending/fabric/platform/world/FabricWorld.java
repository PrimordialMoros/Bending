/*
 * Copyright 2020-2026 Moros
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.CompositeRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
          return AABB.of(min, max);
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
  public boolean isBlockEntity(Position position) {
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
    var min = new Vec3(box.min().x(), box.min().y(), box.min().z());
    var max = new Vec3(box.max().x(), box.max().y(), box.max().z());
    net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(min, max);
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
    return handle().getMinY();
  }

  @Override
  public int maxHeight() {
    return handle().getMaxY();
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
  public BlockRayTrace rayTraceBlocks(Context context) {
    return RayTraceUtil.rayTraceBlocks(context, handle(), this);
  }

  @Override
  public CompositeRayTrace rayTraceEntities(Context context, double range) {
    Entity result = null;
    Vector3d resPos = null;
    double minDistSq = Double.MAX_VALUE;
    var dir = context.dir().normalize().multiply(range);
    var endPoint = context.origin().add(dir);
    var box = AABB.fromRay(context.origin(), dir, context.raySize());
    var aabb = new net.minecraft.world.phys.AABB(box.min().x(), box.min().y(), box.min().z(), box.max().x(), box.max().y(), box.max().z());
    var vec3d1 = new Vec3(context.origin().x(), context.origin().y(), context.origin().z());
    var vec3d2 = new Vec3(endPoint.x(), endPoint.y(), endPoint.z());
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
    return result == null ? RayTrace.miss(endPoint) : RayTrace.hit(resPos, result);
  }

  @Override
  public boolean isDay() {
    return dimension() == Dimension.OVERWORLD && handle().isBrightOutside();
  }

  @Override
  public boolean isNight() {
    return dimension() == Dimension.OVERWORLD && handle().isDarkOutside();
  }

  @Override
  public Entity createEntity(Position pos, EntityType type) {
    if (type == EntityType.PLAYER) {
      throw new IllegalArgumentException("Cannot create a Player.");
    }
    final double x = pos.x();
    final double y = pos.y();
    final double z = pos.z();
    net.minecraft.world.entity.Entity entity = null;
    // Handle edge cases with specialised constructor
    if (type == EntityType.FALLING_BLOCK) {
      entity = FallingBlockEntityAccess.bending$create(handle(), x, y, z, Blocks.SAND.defaultBlockState());
    } else if (type == EntityType.ITEM) {
      entity = new ItemEntity(handle(), x, y, z, Items.STONE.getDefaultInstance());
    }
    if (entity == null) {
      var entityType = BuiltInRegistries.ENTITY_TYPE.getValue(PlatformAdapter.identifier(type.key()));
      entity = Objects.requireNonNull(entityType.create(handle(), EntitySpawnReason.TRIGGERED)); // TODO use different reason?
      entity.snapTo(x, y, z);
    }
    return PlatformAdapter.fromFabricEntity(entity);
  }

  @Override
  public boolean addEntity(Entity entity) {
    return handle().addFreshEntity(PlatformAdapter.toFabricEntity(entity));
  }

  @Override
  public boolean breakNaturally(int x, int y, int z) {
    return handle().destroyBlock(new BlockPos(x, y, z), true);
  }

  @Override
  public Entity dropItem(Position position, ItemSnapshot item, boolean canPickup) {
    var type = PlatformAdapter.toFabricItem(item);
    ItemEntity droppedItem = new ItemEntity(handle(), position.x(), position.y(), position.z(), type);
    if (!canPickup) {
      droppedItem.setNeverPickUp();
    }
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
  public int lightLevel(int x, int y, int z) {
    return handle().getMaxLocalRawBrightness(new BlockPos(x, y, z));
  }

  @Override
  public int blockLightLevel(int x, int y, int z) {
    return handle().getBrightness(LightLayer.BLOCK, new BlockPos(x, y, z));
  }

  @Override
  public int skyLightLevel(int x, int y, int z) {
    return handle().getBrightness(LightLayer.SKY, new BlockPos(x, y, z));
  }

  @Override
  public Dimension dimension() {
    var r = handle().dimensionTypeRegistration();
    if (r.is(BuiltinDimensionTypes.OVERWORLD) || r.is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
      return Dimension.OVERWORLD;
    } else if (r.is(BuiltinDimensionTypes.NETHER)) {
      return Dimension.NETHER;
    } else if (r.is(BuiltinDimensionTypes.END)) {
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
    return ((ChunkMapAccess) handle().getChunkSource().chunkMap).bending$viewDistance();
  }

  @Override
  public Iterable<? extends Audience> audiences() {
    return handle().players();
  }

  @Override
  public Key key() {
    return handle().dimension().identifier();
  }
}
