/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.CollisionCallback;
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
	 * @return {@link #handleEntityCollisions(User, Collider, CollisionCallback, boolean, boolean)} with selfCollision disabled
	 */
	public static boolean handleEntityCollisions(User user, Collider collider, CollisionCallback callback, boolean livingOnly) {
		return handleEntityCollisions(user, collider, callback, livingOnly, false);
	}


	/**
	 * Checks a collider to see if it's hitting any entities near it.
	 * By default it ignores Spectators and invisible armor stands.
	 * @param user the user (needed for self collision and to specify the world in which collisions are checked)
	 * @param collider the collider to check
	 * @param callback the method to  be called for every hit entity
	 * @param livingOnly whether only LivingEntities should be checked
	 * @param selfCollision whether the collider can collider with the user
	 * @return true if it hit at least one entity
	 */
	public static boolean handleEntityCollisions(User user, Collider collider, CollisionCallback callback, boolean livingOnly, boolean selfCollision) {
		final double buffer = 4.0; // Buffer needed to check for nearby entities that have locations outside the check range but still intersect
		Vector3 extent = collider.getHalfExtents().add(new Vector3(buffer, buffer, buffer));
		Vector3 pos = collider.getPosition();
		boolean hit = false;
		for (Entity entity : user.getWorld().getNearbyEntities(pos.toLocation(user.getWorld()), extent.getX(), extent.getY(), extent.getZ())) {
			if (!selfCollision && entity.equals(user.getEntity())) continue;
			if (entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			if (entity instanceof FallingBlock && BendingFallingBlock.manager.isTemp((FallingBlock) entity)) {
				continue;
			}
			if (livingOnly && !(entity instanceof LivingEntity)) {
				continue;
			}
			if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
				continue;
			}
			if (collider.intersects(AABBUtils.getEntityBounds(entity))) {
				hit |= callback.onCollision(entity);
			}
		}
		return hit;
	}
}
