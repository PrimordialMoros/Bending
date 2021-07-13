/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
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

  private Predicate<Entity> entityPredicate = x -> true;

  private RayTrace(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction.normalize();
    range(direction.length());
  }

  public @NonNull RayTrace origin(@NonNull Vector3d origin) {
    this.origin = Objects.requireNonNull(origin);
    return this;
  }

  public @NonNull RayTrace direction(@NonNull Vector3d direction) {
    this.direction = direction.normalize();
    return this;
  }

  public @NonNull RayTrace range(double range) {
    this.range = Math.min(MAX_RANGE, Math.max(0, range));
    return this;
  }

  public @NonNull RayTrace ignoreLiquids(boolean ignoreLiquids) {
    this.ignoreLiquids = ignoreLiquids;
    return this;
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

  public @NonNull RayTrace entityPredicate(@NonNull Predicate<Entity> entityPredicate) {
    this.entityPredicate = Objects.requireNonNull(entityPredicate);
    return this;
  }

  /**
   * Gets the targeted location.
   * @param ignore a filter for unwanted blocks
   * @return the target location
   */
  public @NonNull CompositeResult result(@NonNull World world, @NonNull Predicate<Block> ignore) {
    Ray ray = new Ray(origin, direction.multiply(range));
    double step = 0.5;
    Vector3d increment = direction.multiply(step);
    Set<Block> checked = new HashSet<>(FastMath.ceil(2 * range));
    for (double i = 0.1; i <= range; i += step) {
      Vector3d current = origin.add(direction.multiply(i));
      if (!world.isChunkLoaded((int) current.getX() >> 4, (int) current.getZ() >> 4)) {
        return new CompositeResult(null, current.subtract(increment));
      }
      Block block = current.toBlock(world);
      for (Vector3i vector : VectorMethods.decomposeDiagonals(current, increment)) {
        Block diagonalBlock = block.getRelative(vector.getX(), vector.getY(), vector.getZ());
        if (checked.contains(diagonalBlock) || ignore.test(diagonalBlock)) {
          continue;
        }
        checked.add(diagonalBlock);
        AABB blockBounds = diagonalBlock.isLiquid() ? AABB.BLOCK_BOUNDS.at(new Vector3d(diagonalBlock)) : AABBUtils.blockBounds(diagonalBlock);
        if (blockBounds.intersects(ray)) {
          return new CompositeResult(current, diagonalBlock, null);
        }
      }
    }
    return new CompositeResult(null, ray.origin.add(ray.direction));
  }

  public @NonNull CompositeResult result(@NonNull World world) {
    return result(world, type);
  }

  public @NonNull CompositeResult result(@NonNull World world, @NonNull Type type) {
    Location start = origin.toLocation(world);
    Vector dir = direction.toBukkitVector();
    FluidCollisionMode fluid = ignoreLiquids ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
    Vector3d pos = origin.add(direction.multiply(range));
    switch (type) {
      case COMPOSITE:
        return new CompositeResult(world.rayTrace(start, dir, range, fluid, ignorePassable, raySize, entityPredicate), pos);
      case ENTITY:
        return new CompositeResult(world.rayTraceEntities(start, dir, range, raySize, entityPredicate), pos);
      default:
      case BLOCK:
        return new CompositeResult(world.rayTraceBlocks(start, dir, range, fluid, ignorePassable), pos);
    }
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

    private CompositeResult(RayTraceResult result, Vector3d position) {
      this.position = result == null ? position : new Vector3d(result.getHitPosition());
      this.block = result == null ? null : result.getHitBlock();
      this.entity = result == null ? null : result.getHitEntity();
      hit = block != null || entity != null;
    }

    private CompositeResult(Vector3d position, Block block, Entity entity) {
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
      return entity == null ? position : EntityMethods.entityCenter(entity);
    }

    public @NonNull Vector3d entityEyeLevelOrPosition() {
      if (entity instanceof LivingEntity) {
        return new Vector3d(((LivingEntity) entity).getEyeLocation());
      }
      return position;
    }

    public boolean hit() {
      return hit;
    }
  }
}
