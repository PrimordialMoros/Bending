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

package me.moros.bending.util.collision;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class CollisionUtil {
	/**
	 * @return {@link #handleEntityCollisions(User, Collider, CollisionCallback, boolean, boolean)} with living entities only and selfCollision. earlyEscape disabled
	 */
	public static boolean handleEntityCollisions(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback) {
		return handleEntityCollisions(user, collider, callback, true, false, false);
	}

	/**
	 * @return {@link #handleEntityCollisions(User, Collider, CollisionCallback, boolean, boolean)} with selfCollision and earlyEscape disabled
	 */
	public static boolean handleEntityCollisions(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly) {
		return handleEntityCollisions(user, collider, callback, livingOnly, false, false);
	}

	/**
	 * @return {@link #handleEntityCollisions(User, Collider, CollisionCallback, boolean, boolean, boolean)} with earlyEscape disabled
	 */
	public static boolean handleEntityCollisions(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly, boolean selfCollision) {
		return handleEntityCollisions(user, collider, callback, livingOnly, selfCollision, false);
	}

	/**
	 * Checks a collider to see if it's hitting any entities near it.
	 * By default it ignores Spectators and invisible armor stands.
	 * @param user the user (needed for self collision and to specify the world in which collisions are checked)
	 * @param collider the collider to check
	 * @param callback the method to  be called for every hit entity
	 * @param livingOnly whether only LivingEntities should be checked
	 * @param selfCollision whether the collider can collider with the user
	 * @param earlyEscape if true it will return on the first valid collision callback without evaluating other entities
	 * @return true if it hit at least one entity
	 */
	public static boolean handleEntityCollisions(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly, boolean selfCollision, boolean earlyEscape) {
		final double buffer = 4.0; // Buffer needed to check for nearby entities that have locations outside the check range but still intersect
		Vector3 extent = collider.getHalfExtents().add(new Vector3(buffer, buffer, buffer));
		Vector3 pos = collider.getPosition();
		boolean hit = false;
		for (Entity entity : user.getWorld().getNearbyEntities(pos.toLocation(user.getWorld()), extent.getX(), extent.getY(), extent.getZ())) {
			if (livingOnly && !(entity instanceof LivingEntity)) continue;
			if (!selfCollision && entity.equals(user.getEntity())) continue;
			if (!isValidEntity(entity)) continue;
			if (collider.intersects(AABBUtils.getEntityBounds(entity))) {
				boolean result = callback.onCollision(entity);
				if (earlyEscape && result) return true;
				hit |= result;
			}
		}
		return hit;
	}

	private static boolean isValidEntity(Entity entity) {
		if (entity instanceof Player) {
			return ((Player) entity).getGameMode() != GameMode.SPECTATOR;
		} else if (entity instanceof FallingBlock) {
			return !BendingFallingBlock.manager.isTemp((FallingBlock) entity);
		} else if (entity instanceof ArmorStand) {
			return ((ArmorStand) entity).isVisible();
		}
		return true;
	}

	@FunctionalInterface
	public interface CollisionCallback {
		boolean onCollision(@NonNull Entity e);
	}
}
