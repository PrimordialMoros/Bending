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

package me.moros.bending.model.user;

import java.util.UUID;

import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.raytrace.RayTrace;
import me.moros.bending.util.EntityUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.MainHand;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public interface BukkitUser extends ForwardingAudience.Single, Identity {
  @NonNull LivingEntity entity();

  @Override
  default @NonNull UUID uuid() {
    return entity().getUniqueId();
  }

  default @NonNull Block headBlock() {
    return entity().getEyeLocation().getBlock();
  }

  default @NonNull Block locBlock() {
    return entity().getLocation().getBlock();
  }

  default @NonNull Vector3d location() {
    return new Vector3d(entity().getLocation());
  }

  default @NonNull Vector3d eyeLocation() {
    return new Vector3d(entity().getEyeLocation());
  }

  default @NonNull Vector3d direction() {
    return new Vector3d(entity().getLocation().getDirection());
  }

  default @NonNull Vector3d velocity() {
    return new Vector3d(entity().getVelocity());
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

  default boolean sprinting() {
    return true;
  }

  default void sprinting(boolean sprinting) {
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

  default @Nullable Inventory inventory() {
    return entity() instanceof InventoryHolder holder ? holder.getInventory() : null;
  }

  default boolean isOnGround() {
    return EntityUtil.isOnGround(entity());
  }

  /**
   * @return {@link #rayTrace(double, Class)} with class matching LivingEntities
   */
  default @NonNull RayTrace rayTrace(double range) {
    return rayTrace(range, LivingEntity.class);
  }

  /**
   * Prepare a composite ray trace matching the user's view and filtering the specified class type for entities.
   * @see RayTrace
   */
  default <T extends Entity> @NonNull RayTrace rayTrace(double range, @NonNull Class<T> type) {
    return RayTrace.of(eyeLocation(), direction()).range(range).filterForUser(entity(), type);
  }

  default @NonNull RayTrace rayTrace(@NonNull Vector3d origin, @NonNull Vector3d dir) {
    return rayTrace(origin, dir, LivingEntity.class);
  }

  default <T extends Entity> @NonNull RayTrace rayTrace(@NonNull Vector3d origin, @NonNull Vector3d dir, @NonNull Class<T> type) {
    return RayTrace.of(origin, dir).filterForUser(entity(), type);
  }

  /**
   * Note: The returned value includes an offset and is ideal for showing charging particles.
   * @return a vector which represents the user's main hand location
   * @see #rightSide()
   * @see #leftSide()
   */
  default @NonNull Vector3d mainHandSide() {
    Vector3d dir = direction().multiply(0.4);
    return entity() instanceof Player player ? handSide(player.getMainHand() == MainHand.RIGHT) : eyeLocation().add(dir);
  }

  /**
   * Gets the user's specified hand position.
   * @param right whether to get the right hand
   * @return a vector which represents the user's specified hand location
   */
  default @NonNull Vector3d handSide(boolean right) {
    Vector3d offset = direction().multiply(0.4).add(new Vector3d(0, sneaking() ? 1.2 : 1.575, 0));
    return right ? rightSide().add(offset) : leftSide().add(offset);
  }

  /**
   * Gets the user's right side.
   * @return a vector which represents the user's right side
   */
  default @NonNull Vector3d rightSide() {
    double angle = Math.toRadians(yaw());
    return location().subtract(new Vector3d(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  /**
   * Gets the user's left side.
   * @return a vector which represents the user's left side
   */
  default @NonNull Vector3d leftSide() {
    double angle = Math.toRadians(yaw());
    return location().add(new Vector3d(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.3));
  }

  @Override
  default @NonNull Audience audience() {
    return entity();
  }
}
