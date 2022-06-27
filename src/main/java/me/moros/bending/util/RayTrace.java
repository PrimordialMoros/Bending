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

package me.moros.bending.util;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.internal.NMSUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RayTrace {
  private static final double MAX_RANGE = 100;

  public enum Type {COMPOSITE, ENTITY, BLOCK}

  private Vector3d origin;
  private Vector3d direction;

  private double range;
  private double raySize = 0;

  private boolean ignoreLiquids = true;
  private boolean ignorePassable = true;

  private Type type = Type.BLOCK;

  private Set<Block> ignoreBlocks = Set.of();
  private Predicate<Entity> entityPredicate = x -> true;

  private RayTrace(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction.normalize();
    range(direction.length());
  }

  public @NonNull Vector3d origin() {
    return origin;
  }

  public @NonNull RayTrace origin(@NonNull Vector3d origin) {
    this.origin = Objects.requireNonNull(origin);
    return this;
  }

  public @NonNull Vector3d end() {
    return origin.add(direction.multiply(range));
  }

  public @NonNull RayTrace direction(@NonNull Vector3d direction) {
    this.direction = direction.normalize();
    return this;
  }

  public @NonNull RayTrace range(double range) {
    this.range = Math.min(MAX_RANGE, Math.max(0, range));
    return this;
  }

  public boolean ignoreLiquids() {
    return ignoreLiquids;
  }

  public @NonNull RayTrace ignoreLiquids(boolean ignoreLiquids) {
    this.ignoreLiquids = ignoreLiquids;
    return this;
  }

  public boolean ignorePassable() {
    return ignorePassable;
  }

  public @NonNull RayTrace ignorePassable(boolean ignorePassable) {
    this.ignorePassable = ignorePassable;
    return this;
  }

  public @NonNull RayTrace type(@NonNull Type type) {
    this.type = Objects.requireNonNull(type);
    return this;
  }

  public @NonNull RayTrace raySize(double raySize) {
    this.raySize = Math.max(0, raySize);
    return this;
  }

  public @NonNull Set<@NonNull Block> ignored() {
    return ignoreBlocks;
  }

  public @NonNull RayTrace ignore(@NonNull Set<Block> ignoreBlocks) {
    this.ignoreBlocks = Set.copyOf(ignoreBlocks);
    return this;
  }

  public @NonNull RayTrace filter(@NonNull Predicate<Entity> entityPredicate) {
    this.entityPredicate = Objects.requireNonNull(entityPredicate);
    return this;
  }

  public @NonNull CompositeResult result(@NonNull World world) {
    boolean checkEntities = type != Type.BLOCK;
    Vector3d endPoint = origin.add(direction.multiply(range));
    CompositeResult blockResult = NMSUtil.rayTraceBlocks(this, world);
    double blockHitDistance = blockResult.hit ? origin.distance(blockResult.position) : range;

    CompositeResult entityResult = new CompositeResult(endPoint, null, null);
    if (checkEntities) {
      RayTraceResult eResult = world.rayTraceEntities(origin.toLocation(world), direction.toBukkitVector(), blockHitDistance, raySize, entityPredicate);
      Vector3d pos = eResult == null ? endPoint : new Vector3d(eResult.getHitPosition());
      Entity entity = eResult == null ? null : eResult.getHitEntity();
      entityResult = new CompositeResult(pos, null, entity);
    }
    if (!blockResult.hit) {
      return entityResult;
    }
    if (!entityResult.hit) {
      return blockResult;
    }
    double distSq = origin.distanceSq(entityResult.position);
    if (distSq < (blockHitDistance * blockHitDistance)) {
      return entityResult;
    }
    return blockResult;
  }

  public static @NonNull RayTrace of(@NonNull User user) {
    return new RayTrace(user.eyeLocation(), user.direction());
  }

  public static @NonNull RayTrace of(@NonNull Vector3d origin, @NonNull Vector3d direction) {
    Objects.requireNonNull(origin);
    Objects.requireNonNull(direction);
    return new RayTrace(origin, direction);
  }

  public static final class CompositeResult {
    private final Vector3d position;
    private final Block block;
    private final Entity entity;

    private final boolean hit;

    public CompositeResult(@NonNull Vector3d position, @Nullable Block block, @Nullable Entity entity) {
      this.position = position;
      this.block = block;
      this.entity = entity;
      hit = block != null || entity != null;
    }

    public @NonNull Vector3d position() {
      return position;
    }

    public @Nullable Block block() {
      return block;
    }

    public @Nullable Entity entity() {
      return entity;
    }

    public @NonNull Vector3d entityCenterOrPosition() {
      return entity == null ? position : EntityUtil.entityCenter(entity);
    }

    public @NonNull Vector3d entityEyeLevelOrPosition() {
      if (entity instanceof LivingEntity livingEntity) {
        return new Vector3d(livingEntity.getEyeLocation());
      }
      return position;
    }

    public boolean hit() {
      return hit;
    }
  }
}
