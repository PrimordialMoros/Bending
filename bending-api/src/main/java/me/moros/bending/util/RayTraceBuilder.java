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

import me.moros.bending.adapter.impl.NativeAdapter;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.raytrace.BlockRayTrace;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.EntityRayTrace;
import me.moros.bending.model.raytrace.RayTraceContext;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class RayTraceBuilder {
  public static final double MAX_RANGE = 100;

  private Vector3d origin;
  private Vector3d direction;

  private double range;
  private double raySize = 0;

  private boolean ignoreLiquids = true;
  private boolean ignorePassable = true;

  private Set<Block> ignoreBlocks = Set.of();
  private Predicate<Entity> entityPredicate = x -> true;

  private RayTraceBuilder(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction.normalize();
    range(direction.length());
  }

  public @NonNull RayTraceBuilder origin(@NonNull Vector3d origin) {
    this.origin = Objects.requireNonNull(origin);
    return this;
  }

  public @NonNull RayTraceBuilder direction(@NonNull Vector3d direction) {
    this.direction = direction.normalize();
    return this;
  }

  public @NonNull Vector3d end() {
    return origin.add(direction.multiply(range));
  }

  public @NonNull RayTraceBuilder range(double range) {
    this.range = Math.min(MAX_RANGE, Math.max(0, range));
    return this;
  }

  public @NonNull RayTraceBuilder raySize(double raySize) {
    this.raySize = Math.max(0, raySize);
    return this;
  }

  public @NonNull RayTraceBuilder ignoreLiquids(boolean ignoreLiquids) {
    this.ignoreLiquids = ignoreLiquids;
    return this;
  }

  public @NonNull RayTraceBuilder ignorePassable(boolean ignorePassable) {
    this.ignorePassable = ignorePassable;
    return this;
  }

  public @NonNull RayTraceBuilder ignore(@NonNull Set<@NonNull Block> ignoreBlocks) {
    this.ignoreBlocks = Set.copyOf(ignoreBlocks);
    return this;
  }

  public @NonNull RayTraceBuilder filter(@NonNull Predicate<Entity> entityPredicate) {
    this.entityPredicate = Objects.requireNonNull(entityPredicate);
    return this;
  }

  public <T extends Entity> @NonNull RayTraceBuilder filterForUser(@NonNull Entity source, @NonNull Class<T> type) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(type);
    return filter(e -> userPredicate(e, source, type));
  }

  public @NonNull BlockRayTrace blocks(@NonNull World world) {
    return result(world, false);
  }

  public @NonNull EntityRayTrace entities(@NonNull World world) {
    return result(world, true);
  }

  private CompositeRayTrace result(World world, boolean checkEntities) {
    Vector3d endPoint = end();
    RayTraceContext context = new RayTraceContext(origin, endPoint, ignoreLiquids, ignorePassable, ignoreBlocks);
    CompositeRayTrace blockResult = NativeAdapter.instance().rayTraceBlocks(context, world);
    double blockHitDistance = blockResult.hit() ? origin.distance(blockResult.position()) : range;

    CompositeRayTrace entityResult = CompositeRayTrace.miss(endPoint);
    if (checkEntities) {
      Location start = origin.toLocation(world);
      Vector dir = direction.toBukkitVector();
      org.bukkit.util.RayTraceResult eResult = world.rayTraceEntities(start, dir, blockHitDistance, raySize, entityPredicate);
      if (eResult != null) {
        Entity entity = eResult.getHitEntity();
        if (entity != null) {
          entityResult = CompositeRayTrace.hit(new Vector3d(eResult.getHitPosition()), entity);
        }
      }
    }
    if (!blockResult.hit()) {
      return entityResult;
    }
    if (!entityResult.hit()) {
      return blockResult;
    }
    double distSq = origin.distanceSq(entityResult.position());
    if (distSq < (blockHitDistance * blockHitDistance)) {
      return entityResult;
    }
    return blockResult;
  }

  public static @NonNull RayTraceBuilder of(@NonNull Vector3d origin, @NonNull Vector3d direction) {
    Objects.requireNonNull(origin);
    Objects.requireNonNull(direction);
    return new RayTraceBuilder(origin, direction);
  }

  private static <T extends Entity> boolean userPredicate(Entity check, Entity entity, Class<T> type) {
    if (check instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }
    return type.isInstance(check) && !check.equals(entity);
  }
}
