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

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.raytrace.BlockRayTrace;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.EntityRayTrace;
import me.moros.bending.model.raytrace.RayTrace.Context;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to easily cast ray traces.
 */
public final class RayTraceBuilder {
  public static final double MIN_RANGE = 1;
  public static final double MAX_RANGE = 100;

  private Vector3d origin;
  private Vector3d direction;

  private double range;
  private double raySize = 0;

  private boolean ignoreLiquids = true;
  private boolean ignorePassable = true;

  private Set<Position> ignore = Set.of();
  private Predicate<Entity> entityPredicate = x -> true;

  private RayTraceBuilder(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction.normalize();
    range(direction.length());
  }

  /**
   * Override the raytrace origin.
   * @param origin the new origin
   * @return the modified builder
   */
  public RayTraceBuilder origin(Vector3d origin) {
    this.origin = Objects.requireNonNull(origin);
    return this;
  }

  /**
   * Override the raytrace direction.
   * @param direction the new direction
   * @return the modified builder
   */
  public RayTraceBuilder direction(Vector3d direction) {
    this.direction = direction.normalize();
    return this;
  }

  /**
   * Override the raytrace range.
   * <p>Note: Range is clamped at [{@value MIN_RANGE}, {@value MAX_RANGE}].
   * @param range the new range
   * @return the modified builder
   */
  public RayTraceBuilder range(double range) {
    this.range = FastMath.clamp(range, MIN_RANGE, MAX_RANGE);
    return this;
  }

  /**
   * Override the raytrace ray size.
   * Ray size effectively grows the ray's collider when checked against entities.
   * Default value is 0.
   * @param raySize the new non-negative ray size
   * @return the modified builder
   */
  public RayTraceBuilder raySize(double raySize) {
    this.raySize = Math.max(0, raySize);
    return this;
  }

  /**
   * Override whether the raytrace should ignore liquids.
   * Default value is true.
   * @param ignoreLiquids the new value
   * @return the modified builder
   */
  public RayTraceBuilder ignoreLiquids(boolean ignoreLiquids) {
    this.ignoreLiquids = ignoreLiquids;
    return this;
  }

  /**
   * Override whether the raytrace should ignore passable blocks (blocks that the player can move through).
   * Default value is true.
   * @param ignorePassable the new value
   * @return the modified builder
   */
  public RayTraceBuilder ignorePassable(boolean ignorePassable) {
    this.ignorePassable = ignorePassable;
    return this;
  }

  /**
   * Define a block the raytrace should ignore.
   * @param ignore the block to ignore if not null
   * @return the modified builder
   * @see #ignore(Set)
   */
  public RayTraceBuilder ignore(@Nullable Block ignore) {
    this.ignore = ignore == null ? Set.of() : Set.of(Vector3i.from(ignore));
    return this;
  }

  /**
   * Define a position the raytrace should ignore.
   * @param ignore the position to ignore
   * @return the modified builder
   * @see #ignore(Set)
   */
  public RayTraceBuilder ignore(Position ignore) {
    return ignore(Set.of(ignore));
  }

  /**
   * Define a set of positions the raytrace should ignore.
   * Default value is an empty set.
   * @param ignore the new set of blocks to ignore
   * @return the modified builder
   */
  public RayTraceBuilder ignore(Set<Position> ignore) {
    this.ignore = Set.copyOf(ignore);
    return this;
  }

  /**
   * Define a predicate of entities to filter when casting the raytrace. All other entities will be ignored.
   * Default value is colliding with all entities.
   * @param entityPredicate the new predicate
   * @return the modified builder
   */
  public RayTraceBuilder filter(Predicate<Entity> entityPredicate) {
    this.entityPredicate = Objects.requireNonNull(entityPredicate);
    return this;
  }

  /**
   * Utility method to apply a complex entity {@link #filter(Predicate)}.
   * When set, the raytrace will ignore  the specified user and only check entities that match the specified class type.
   * Moreover, players in spectator mode will also be ignored.
   * @param source the user to ignore
   * @param type the type of entities to filter
   * @return the modified builder
   */
  public <T extends Entity> RayTraceBuilder filterForUser(Entity source, Class<T> type) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(type);
    return filter(e -> userPredicate(e, source, type));
  }

  /**
   * Build and cast the raytrace checking only blocks.
   * @param world the world to cast the raytrace in
   * @return the result
   */
  public BlockRayTrace blocks(World world) {
    return result(world, false);
  }

  /**
   * Build and cast the raytrace checking both blocks and entities.
   * @param world the world to cast the raytrace in
   * @return the result
   */
  public EntityRayTrace entities(World world) {
    return result(world, true);
  }

  private CompositeRayTrace result(World world, boolean checkEntities) {
    Objects.requireNonNull(world);
    Vector3d endPoint = origin.add(direction.multiply(range));
    Context context = new Context(origin, endPoint, ignoreLiquids, ignorePassable, ignore);
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
          entityResult = CompositeRayTrace.hit(Vector3d.from(eResult.getHitPosition()), entity);
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

  /**
   * Create a new builder instance using the specified origin and direction.
   * <p>Note: The range is calculated based on the length of the direction vector.
   * @param origin the origin of the raytrace
   * @param direction the direction of the raytrace
   * @return a new builder instance
   */
  public static RayTraceBuilder of(Vector3d origin, Vector3d direction) {
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
