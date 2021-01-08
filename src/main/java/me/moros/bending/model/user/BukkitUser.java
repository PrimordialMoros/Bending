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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;

public interface BukkitUser extends CommandUser {
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
}
