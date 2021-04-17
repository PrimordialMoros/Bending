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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.methods.EntityMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
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

import java.util.Optional;
import java.util.function.Predicate;

public interface BukkitUser {
	@NonNull LivingEntity getEntity();

	default @NonNull Block getHeadBlock() {
		return getEntity().getEyeLocation().getBlock();
	}

	default @NonNull Block getLocBlock() {
		return getEntity().getLocation().getBlock();
	}

	default @NonNull Vector3 getLocation() {
		return new Vector3(getEntity().getLocation());
	}

	default @NonNull Vector3 getEyeLocation() {
		return new Vector3(getEntity().getEyeLocation());
	}

	default @NonNull Vector3 getDirection() {
		return new Vector3(getEntity().getLocation().getDirection());
	}

	default @NonNull Vector3 getVelocity() {
		return new Vector3(getEntity().getVelocity());
	}

	default int getYaw() {
		return (int) getEntity().getLocation().getYaw();
	}

	default int getPitch() {
		return (int) getEntity().getLocation().getPitch();
	}

	default @NonNull World getWorld() {
		return getEntity().getWorld();
	}

	default @NonNull Ray getRay() {
		return new Ray(getEyeLocation(), getDirection());
	}

	default @NonNull Ray getRay(double range) {
		return new Ray(getEyeLocation(), getDirection().scalarMultiply(range));
	}

	/**
	 * @return {@link Entity#isValid}
	 */
	default boolean isValid() {
		return getEntity().isValid();
	}

	/**
	 * @return {@link Entity#isDead()}
	 */
	default boolean isDead() {
		return getEntity().isDead();
	}

	default boolean isSpectator() {
		return false;
	}

	default boolean isSneaking() {
		return true; // Non-players are always considered sneaking so they can charge abilities.
	}

	default boolean getAllowFlight() {
		return true;
	}

	default boolean isFlying() {
		return false;
	}

	default void setAllowFlight(boolean allow) {
	}

	default void setFlying(boolean flying) {
	}

	default Optional<Inventory> getInventory() {
		if (getEntity() instanceof InventoryHolder) return Optional.of(((InventoryHolder) getEntity()).getInventory());
		return Optional.empty();
	}

	/**
	 * Gets the targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, Predicate)
	 */
	default Optional<LivingEntity> getTargetEntity(double range) {
		return getTargetEntity(range, LivingEntity.class);
	}

	/**
	 * Gets the targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
	 */
	default Optional<LivingEntity> getTargetEntity(double range, int raySize) {
		return getTargetEntity(range, raySize, LivingEntity.class);
	}

	/**
	 * Gets the targeted entity (predicate is used to ignore the user's entity) and filter to specified type.
	 * @see World#rayTraceEntities(Location, Vector, double, Predicate)
	 */
	default <T extends Entity> Optional<T> getTargetEntity(double range, @NonNull Class<T> type) {
		RayTraceResult result = getWorld().rayTraceEntities(getEntity().getEyeLocation(), getEntity().getLocation().getDirection(), range, e -> !e.equals(getEntity()));
		if (result == null) return Optional.empty();
		Entity entity = result.getHitEntity();
		return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
	}

	/**
	 * Gets the targeted entity (predicate is used to ignore the user's entity) and filter to specified type.
	 * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
	 */
	default <T extends Entity> Optional<T> getTargetEntity(double range, int raySize, @NonNull Class<T> type) {
		RayTraceResult result = getWorld().rayTraceEntities(getEntity().getEyeLocation(), getEntity().getLocation().getDirection(), range, raySize, e -> !e.equals(getEntity()));
		if (result == null) return Optional.empty();
		Entity entity = result.getHitEntity();
		return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
	}

	default boolean isOnGround() {
		return EntityMethods.isOnGround(getEntity());
	}

	/**
	 * Note: The returned value includes an offset and is ideal for showing charging particles.
	 * @return a vector which represents the user's main hand location
	 * @see #getRightSide()
	 * @see #getLeftSide()
	 */
	default @NonNull Vector3 getMainHandSide() {
		Vector3 dir = getDirection().scalarMultiply(0.4);
		if (getEntity() instanceof Player) {
			return getHandSide(((Player) getEntity()).getMainHand() == MainHand.RIGHT);
		}
		return getEyeLocation().add(dir);
	}

	/**
	 * Gets the user's specified hand position.
	 * @param right whether to get the right hand
	 * @return a vector which represents the user's specified hand location
	 */
	default @NonNull Vector3 getHandSide(boolean right) {
		Vector3 offset = getDirection().scalarMultiply(0.4).add(new Vector3(0, 1.2, 0));
		return right ? getRightSide().add(offset) : getLeftSide().add(offset);
	}

	/**
	 * Gets the user's right side.
	 * @return a vector which represents the user's right side
	 */
	default @NonNull Vector3 getRightSide() {
		double angle = FastMath.toRadians(getYaw());
		return getLocation().subtract(new Vector3(FastMath.cos(angle), 0, FastMath.sin(angle)).normalize().scalarMultiply(0.3));
	}

	/**
	 * Gets the user's left side.
	 * @return a vector which represents the user's left side
	 */
	default @NonNull Vector3 getLeftSide() {
		double angle = FastMath.toRadians(getYaw());
		return getLocation().add(new Vector3(FastMath.cos(angle), 0, FastMath.sin(angle)).normalize().scalarMultiply(0.3));
	}
}
