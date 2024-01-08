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

package me.moros.bending.sponge.platform.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.raytrace.CompositeRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.sponge.mixin.accessor.FallingBlockEntityAccess;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.SpongeDataHolder;
import me.moros.bending.sponge.platform.block.LockableImpl;
import me.moros.bending.sponge.platform.particle.ParticleMapper;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.server.ServerWorld;

public record SpongeWorld(ServerWorld handle) implements World {
  private ServerLevel nms() {
    return (ServerLevel) handle();
  }

  @Override
  public BlockType getBlockType(int x, int y, int z) {
    return getBlockState(x, y, z).type();
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    return PlatformAdapter.fromSpongeData(handle().block(x, y, z));
  }

  @Override
  public AABB blockBounds(int x, int y, int z) {
    var b = (net.minecraft.world.level.block.state.BlockState) handle().block(x, y, z);
    var shape = b.getShape(nms(), new BlockPos(x, y, z));
    if (!shape.isEmpty()) {
      var box = shape.bounds();
      if (box.getSize() > 0) {
        Vector3d min = Vector3d.of(x + box.minX, y + box.minY, z + box.minZ);
        Vector3d max = Vector3d.of(x + box.maxX, y + box.maxY, z + box.maxZ);
        return AABB.of(min, max);
      }
    }
    return AABB.dummy();
  }

  @Override
  public DataHolder blockMetadata(int x, int y, int z) {
    return new SpongeDataHolder(handle().location(x, y, z));
  }

  @Override
  public boolean isTileEntity(Position position) {
    return handle().blockEntity(position.blockX(), position.blockY(), position.blockZ()).isPresent();
  }

  @Override
  public @Nullable Lockable containerLock(Position position) {
    return handle().blockEntity(position.blockX(), position.blockY(), position.blockZ())
      .filter(tile -> tile.supports(Keys.LOCK_TOKEN))
      .map(LockableImpl::new).orElse(null);
  }

  @Override
  public boolean setBlockState(int x, int y, int z, BlockState state) {
    handle().setBlock(x, y, z, PlatformAdapter.toSpongeData(state));
    return true;
  }

  @Override
  public List<Entity> nearbyEntities(AABB box, Predicate<Entity> predicate, int limit) {
    var min = org.spongepowered.math.vector.Vector3d.from(box.min().x(), box.min().y(), box.min().z());
    var max = org.spongepowered.math.vector.Vector3d.from(box.max().x(), box.max().y(), box.max().z());
    org.spongepowered.api.util.AABB aabb = org.spongepowered.api.util.AABB.of(min, max);
    List<Entity> entities = new ArrayList<>();
    for (var spongeEntity : handle().entities(aabb)) {
      Entity entity = PlatformAdapter.fromSpongeEntity(spongeEntity);
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
    return handle().properties().name();
  }

  @Override
  public int minHeight() {
    return handle().min().y();
  }

  @Override
  public int maxHeight() {
    return handle().maximumHeight();
  }

  @Override
  public <T> void spawnParticle(ParticleContext<T> context) {
    var effect = ParticleMapper.mapParticleEffect(context);
    if (effect != null) {
      var vec = org.spongepowered.math.vector.Vector3d.from(context.position().x(), context.position().y(), context.position().z());
      handle().spawnParticles(effect, vec, 128);
    }
  }

  @Override
  public CompositeRayTrace rayTraceEntities(Context context, double range) {
    Entity result = null;
    Vector3d resPos = null;
    double minDistSq = Double.MAX_VALUE;
    var dir = context.dir().normalize().multiply(range);
    var box = AABB.fromRay(context.origin(), dir, context.raySize());
    var size = box.max().subtract(box.min());
    if (size.x() != 0 && size.y() != 0 && size.z() != 0) { // Box has no volume?
      var aabb = org.spongepowered.api.util.AABB.of(box.min().x(), box.min().y(), box.min().z(), box.max().x(), box.max().y(), box.max().z());
      var vec3d1 = new Vec3(context.origin().x(), context.origin().y(), context.origin().z());
      var vec3d2 = new Vec3(dir.x(), dir.y(), dir.z());
      for (var spongeEntity : handle().entities(aabb)) {
        var pos = ((net.minecraft.world.entity.Entity) spongeEntity).getBoundingBox().clip(vec3d1, vec3d2).orElse(null);
        if (pos != null) {
          Entity entity = PlatformAdapter.fromSpongeEntity(spongeEntity);
          double distSq = pos.distanceToSqr(vec3d1);
          if (distSq < minDistSq) {
            result = entity;
            resPos = Vector3d.of(pos.x(), pos.y(), pos.z());
            minDistSq = distSq;
          }
        }
      }
    }
    return result == null ? RayTrace.miss(context.endPoint()) : RayTrace.hit(resPos, result);
  }

  @Override
  public boolean isDay() {
    return dimension() == Dimension.OVERWORLD && nms().isDay();
  }

  @Override
  public boolean isNight() {
    return dimension() == Dimension.OVERWORLD && nms().isNight();
  }

  @Override
  public boolean breakNaturally(int x, int y, int z) {
    return handle().destroyBlock(org.spongepowered.math.vector.Vector3i.from(x, y, z), true);
  }

  @Override
  public Entity dropItem(Position pos, ItemSnapshot item, boolean canPickup) {
    var vec = org.spongepowered.math.vector.Vector3d.from(pos.x(), pos.y(), pos.z());
    Item droppedItem = handle().createEntity(EntityTypes.ITEM, vec);
    droppedItem.item().set(PlatformAdapter.toSpongeItemSnapshot(item));
    droppedItem.offer(Keys.INFINITE_PICKUP_DELAY, true);
    handle().spawnEntity(droppedItem);
    return PlatformAdapter.fromSpongeEntity(droppedItem);
  }

  @Override
  public Entity createFallingBlock(Position pos, BlockState state, boolean gravity) {
    var data = (net.minecraft.world.level.block.state.BlockState) PlatformAdapter.toSpongeData(state);
    var fabricEntity = FallingBlockEntityAccess.bending$create(nms(), pos.x(), pos.y(), pos.z(), data);
    fabricEntity.time = 1; // Is this needed?
    fabricEntity.setNoGravity(!gravity);
    fabricEntity.dropItem = false;
    ((FallingBlockEntityAccess) fabricEntity).bending$cancelDrop(true);
    nms().addFreshEntity(fabricEntity);
    return PlatformAdapter.fromSpongeEntity((org.spongepowered.api.entity.Entity) fabricEntity);
  }

  @Override
  public int lightLevel(int x, int y, int z) {
    return nms().getMaxLocalRawBrightness(new BlockPos(x, y, z));
  }

  @Override
  public Dimension dimension() {
    var t = handle().worldType();
    if (t == WorldTypes.OVERWORLD.get() || t == WorldTypes.OVERWORLD_CAVES.get()) {
      return Dimension.OVERWORLD;
    } else if (t == WorldTypes.THE_NETHER.get()) {
      return Dimension.NETHER;
    } else if (t == WorldTypes.THE_END.get()) {
      return Dimension.END;
    } else {
      return Dimension.CUSTOM;
    }
  }

  @Override
  public CompletableFuture<?> loadChunkAsync(int x, int z) {
    return nms().getChunkSource().getChunkFuture(x, z, ChunkStatus.EMPTY, false);
  }

  @Override
  public int viewDistance() {
    return handle().properties().viewDistance();
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return handle().audiences();
  }

  @Override
  public @NonNull Key key() {
    return PlatformAdapter.fromRsk(handle().key());
  }
}
