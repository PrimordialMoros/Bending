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

package me.moros.bending.model.user;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface BukkitUser extends ForwardingAudience.Single {
  @NonNull LivingEntity entity();

  default @NonNull Block headBlock() {
    return entity().getEyeLocation().getBlock();
  }

  default @NonNull Block locBlock() {
    return entity().getLocation().getBlock();
  }

  default @NonNull Vector3 location() {
    return new Vector3(entity().getLocation());
  }

  default @NonNull Vector3 eyeLocation() {
    return new Vector3(entity().getEyeLocation());
  }

  default @NonNull Vector3 direction() {
    return new Vector3(entity().getLocation().getDirection());
  }

  default @NonNull Vector3 velocity() {
    return new Vector3(entity().getVelocity());
  }

  default int yaw() {
    return (int) entity().getLocation().getYaw();
  }

  default int pitch() {
    return (int) entity().getLocation().getPitch();
  }

  default @NonNull World world() {
    return entity().getWorld();
  }

  default @NonNull Ray ray() {
    return new Ray(eyeLocation(), direction());
  }

  default @NonNull Ray ray(double range) {
    return new Ray(eyeLocation(), direction().multiply(range));
  }

  /**
   * @return {@link Entity#isValid}
   */
  default boolean valid() {
    return entity().isValid();
  }

  /**
   * @return {@link Entity#isDead()}
   */
  default boolean dead() {
    return entity().isDead();
  }

  default boolean spectator() {
    return false;
  }

  default boolean sneaking() {
    return true; // Non-players are always considered sneaking so they can charge abilities.
  }

  default boolean allowFlight() {
    return true;
  }

  default void allowFlight(boolean allow) {
  }

  default boolean flying() {
    return false;
  }

  default void flying(boolean flying) {
  }

  default Optional<Inventory> inventory() {
    if (entity() instanceof InventoryHolder) {
      return Optional.of(((InventoryHolder) entity()).getInventory());
    }
    return Optional.empty();
  }

  default boolean isOnGround() {
    return EntityMethods.isOnGround(entity());
  }

  /**
   * Gets the targeted entity (predicate is used to ignore the user's entity).
   * @see World#rayTraceEntities(Location, Vector, double, Predicate)
   */
  default Optional<LivingEntity> rayTraceEntity(double range) {
    return rayTraceEntity(range, 0, LivingEntity.class);
  }

  /**
   * Gets the targeted entity (predicate is used to ignore the user's entity).
   * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
   */
  default Optional<LivingEntity> rayTraceEntity(double range, int raySize) {
    return rayTraceEntity(range, raySize, LivingEntity.class);
  }

  /**
   * Gets the targeted entity (predicate is used to ignore the user's entity) and filter to specified type.
   * @see World#rayTraceEntities(Location, Vector, double, Predicate)
   */
  default <T extends Entity> Optional<T> rayTraceEntity(double range, @NonNull Class<T> type) {
    return rayTraceEntity(range, 0, type);
  }

  /**
   * Gets the targeted entity (predicate is used to ignore the user's entity) and filter to specified type.
   * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
   */
  default <T extends Entity> Optional<T> rayTraceEntity(double range, int raySize, @NonNull Class<T> type) {
    Predicate<Entity> predicate = e -> type.isInstance(e) && !e.equals(entity());
    RayTraceResult result = world().rayTraceEntities(entity().getEyeLocation(), entity().getLocation().getDirection(), range, raySize, predicate);
    if (result == null) {
      return Optional.empty();
    }
    Entity entity = result.getHitEntity();
    return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
  }

  /**
   * @return {@link #rayTrace(double, boolean)} ignoring liquids and passable blocks
   */
  default @NonNull Vector3 rayTrace(double range) {
    return rayTrace(range, true);
  }

  /**
   * Gets the targeted location.
   * <p> Note: Passable blocks are ignored by default.
   * @param range the range for the check
   * @param ignoreLiquids whether liquids should be ignored
   * @return the target location
   */
  default @NonNull Vector3 rayTrace(double range, boolean ignoreLiquids) {
    Location origin = entity().getEyeLocation();
    Vector dir = entity().getLocation().getDirection();
    FluidCollisionMode fluid = ignoreLiquids ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
    RayTraceResult result = world().rayTraceBlocks(origin, dir, range, fluid, true);
    if (result == null) {
      Ray ray = ray(range);
      return ray.origin.add(ray.direction);
    }
    return new Vector3(result.getHitPosition());
  }

  /**
   * Gets the targeted location.
   * <p> Note: Passable blocks are ignored by default.
   * @param range the range for the check
   * @param ignore a filter for unwanted blocks
   * @return the target location
   */
  default @NonNull Vector3 rayTrace(double range, @NonNull Predicate<Block> ignore) {
    Vector3 dir = direction();
    Ray ray = new Ray(eyeLocation(), dir.multiply(range));
    double step = 0.5;
    Vector3 increment = dir.multiply(step);
    Set<Block> checked = new HashSet<>(NumberConversions.ceil(2 * range));
    for (double i = 0.1; i <= range; i += step) {
      Vector3 current = ray.origin.add(dir.multiply(i));
      IntVector vec = current.toIntVector();
      if (!world().isChunkLoaded(vec.x >> 4, vec.z >> 4)) {
        return current.subtract(increment);
      }
      Block block = current.toBlock(world());
      for (IntVector intVector : VectorMethods.decomposeDiagonals(current, increment)) {
        Block diagonalBlock = block.getRelative(intVector.x, intVector.y, intVector.z);
        if (checked.contains(diagonalBlock) || ignore.test(diagonalBlock)) {
          continue;
        }
        checked.add(diagonalBlock);
        AABB blockBounds = diagonalBlock.isLiquid() ? AABB.BLOCK_BOUNDS.at(new Vector3(diagonalBlock)) : AABBUtils.blockBounds(diagonalBlock);
        if (blockBounds.intersects(ray)) {
          return current;
        }
      }
    }
    return ray.origin.add(ray.direction);
  }

  /**
   * Note: The returned value includes an offset and is ideal for showing charging particles.
   * @return a vector which represents the user's main hand location
   * @see #rightSide()
   * @see #leftSide()
   */
  default @NonNull Vector3 mainHandSide() {
    Vector3 dir = direction().multiply(0.4);
    if (entity() instanceof Player) {
      return handSide(((Player) entity()).getMainHand() == MainHand.RIGHT);
    }
    return eyeLocation().add(dir);
  }

  /**
   * Gets the user's specified hand position.
   * @param right whether to get the right hand
   * @return a vector which represents the user's specified hand location
   */
  default @NonNull Vector3 handSide(boolean right) {
    Vector3 offset = direction().multiply(0.4).add(new Vector3(0, 1.2, 0));
    return right ? rightSide().add(offset) : leftSide().add(offset);
  }

  /**
   * Gets the user's right side.
   * @return a vector which represents the user's right side
   */
  default @NonNull Vector3 rightSide() {
    double angle = Math.toRadians(yaw());
    return location().subtract(new Vector3(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  /**
   * Gets the user's left side.
   * @return a vector which represents the user's left side
   */
  default @NonNull Vector3 leftSide() {
    double angle = Math.toRadians(yaw());
    return location().add(new Vector3(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  @Override
  default @NonNull Audience audience() {
    return entity();
  }
}
