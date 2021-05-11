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

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.MainHand;
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
    return rayTraceEntity(range, LivingEntity.class);
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
    Predicate<Entity> predicate = e -> type.isInstance(e) && !e.equals(entity());
    RayTraceResult result = world().rayTraceEntities(entity().getEyeLocation(), entity().getLocation().getDirection(), range, predicate);
    if (result == null) {
      return Optional.empty();
    }
    Entity entity = result.getHitEntity();
    return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
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
   * @return {@link #rayTrace(double, Set)} with an empty material set and ignoreLiquids = true
   */
  default @NonNull Vector3 rayTrace(double range) {
    return rayTrace(range, Set.of(), true);
  }

  /**
   * @return {@link #rayTrace(double, Set, boolean)} with an empty material set
   */
  default @NonNull Vector3 rayTrace(double range, boolean ignoreLiquids) {
    return rayTrace(range, Set.of(), ignoreLiquids);
  }

  /**
   * @return {@link #rayTrace(double, Set, boolean)} with ignoreLiquids = true
   */
  default @NonNull Vector3 rayTrace(double range, @NonNull Set<@NonNull Material> ignored) {
    return rayTrace(range, ignored, true);
  }

  /**
   * Gets the targeted location.
   * <p> Note: {@link Ray#direction} is a {@link Vector3} and its length provides the range for the check.
   * @param range the range for the check
   * @param ignored an extra set of materials that will be ignored (transparent materials are already ignored)
   * @param ignoreLiquids whether liquids should be ignored for collisions
   * @return the target location
   */
  default @NonNull Vector3 rayTrace(double range, @NonNull Set<@NonNull Material> ignored, boolean ignoreLiquids) {
    Ray ray = ray(range);
    Vector3 dir = direction();
    for (int i = 1; i <= range; i++) {
      Vector3 current = ray.origin.add(dir.multiply(i));
      for (Block block : BlockMethods.combineFaces(current.toBlock(world()))) {
        if (MaterialUtil.isTransparent(block) || ignored.contains(block.getType())) {
          continue;
        }
        AABB blockBounds = (block.isLiquid() && !ignoreLiquids) ? AABB.BLOCK_BOUNDS.at(new Vector3(block)) : AABBUtils.blockBounds(block);
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
