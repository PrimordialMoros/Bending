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

package me.moros.bending.api.collision.raytrace;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.world.World;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to easily cast ray traces.
 */
public final class ContextBuilder {
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

  ContextBuilder(Vector3d origin, Vector3d direction) {
    this.origin = origin;
    this.direction = direction.normalize();
    range(direction.length());
  }

  /**
   * Override the raytrace origin.
   * @param origin the new origin
   * @return the modified builder
   */
  public ContextBuilder origin(Vector3d origin) {
    this.origin = Objects.requireNonNull(origin);
    return this;
  }

  /**
   * Override the raytrace direction.
   * @param direction the new direction
   * @return the modified builder
   */
  public ContextBuilder direction(Vector3d direction) {
    this.direction = direction.normalize();
    return this;
  }

  /**
   * Override the raytrace range.
   * <p>Note: Range is clamped at [{@value MIN_RANGE}, {@value MAX_RANGE}].
   * @param range the new range
   * @return the modified builder
   */
  public ContextBuilder range(double range) {
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
  public ContextBuilder raySize(double raySize) {
    this.raySize = Math.max(0, raySize);
    return this;
  }

  /**
   * Override whether the raytrace should ignore liquids.
   * Default value is true.
   * @param ignoreLiquids the new value
   * @return the modified builder
   */
  public ContextBuilder ignoreLiquids(boolean ignoreLiquids) {
    this.ignoreLiquids = ignoreLiquids;
    return this;
  }

  /**
   * Override whether the raytrace should ignore passable blocks (blocks that the player can move through).
   * Default value is true.
   * @param ignorePassable the new value
   * @return the modified builder
   */
  public ContextBuilder ignorePassable(boolean ignorePassable) {
    this.ignorePassable = ignorePassable;
    return this;
  }

  /**
   * Define a position the raytrace should ignore.
   * @param ignore the block to ignore if not null
   * @return the modified builder
   * @see #ignore(Set)
   */
  public ContextBuilder ignore(@Nullable Position ignore) {
    this.ignore = ignore == null ? Set.of() : Set.of(ignore);
    return this;
  }

  /**
   * Define a set of positions the raytrace should ignore.
   * Default value is an empty set.
   * @param ignore the new set of blocks to ignore
   * @return the modified builder
   */
  public ContextBuilder ignore(Set<Position> ignore) {
    this.ignore = Set.copyOf(ignore);
    return this;
  }

  /**
   * Define a predicate of entities to filter when casting the raytrace. All other entities will be ignored.
   * Default value is colliding with all entities.
   * @param entityPredicate the new predicate
   * @return the modified builder
   */
  public ContextBuilder filter(Predicate<Entity> entityPredicate) {
    this.entityPredicate = Objects.requireNonNull(entityPredicate);
    return this;
  }

  /**
   * Utility method to apply a complex entity {@link #filter(Predicate)}.
   * When set, the raytrace will ignore the specified user and only check living entities.
   * Moreover, players in spectator mode will also be ignored.
   * @param source the user to ignore
   * @return the modified builder
   */
  public ContextBuilder filterForUser(Entity source) {
    Objects.requireNonNull(source);
    return filter(e -> userPredicate(e, source));
  }

  /**
   * Build and cast the raytrace checking only blocks.
   * @param world the world to cast the raytrace in
   * @return the result
   */
  public BlockRayTrace blocks(World world) {
    return world.rayTraceBlocks(build());
  }

  /**
   * Build and cast the raytrace checking both blocks and entities.
   * @param world the world to cast the raytrace in
   * @return the result
   */
  public CompositeRayTrace cast(World world) {
    return world.rayTrace(build());
  }

  /**
   * Build a ray trace context from this builder.
   * @return the constructed context
   */
  public Context build() {
    Vector3d endPoint = origin.add(direction.multiply(range));
    return new ContextImpl(origin, endPoint, range, raySize, ignoreLiquids, ignorePassable, deepCopy(ignore), entityPredicate);
  }

  private static Set<Vector3i> deepCopy(Collection<Position> col) {
    // Make new vector3i instances to ensure equality/hashcode checks are consistent
    return col.stream().map(p -> Vector3i.of(p.blockX(), p.blockY(), p.blockZ())).collect(Collectors.toUnmodifiableSet());
  }

  private static boolean userPredicate(Entity check, Entity entity) {
    if (check instanceof Player player && player.gamemode() == GameMode.SPECTATOR) {
      return false;
    }
    return check instanceof LivingEntity && !check.uuid().equals(entity.uuid());
  }
}
