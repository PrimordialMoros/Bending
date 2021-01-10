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

package me.moros.bending.util.methods;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.inventory.MainHand;

/**
 * Utility class with useful {@link User} related methods.
 */
public final class UserMethods {
	public static final Vector3 PLAYER_OFFSET = new Vector3(0, 1.2, 0);

	private static boolean isMainHandRightSide(BendingPlayer user) {
		return user.getEntity().getMainHand() == MainHand.RIGHT;
	}

	/**
	 * Note: The returned value includes an offset and is ideal for showing charging particles.
	 * @param user the user to check
	 * @return a vector which represents the user's main hand location
	 * @see #getRightSide(User)
	 * @see #getLeftSide(User)
	 */
	public static Vector3 getMainHandSide(@NonNull User user) {
		Vector3 dir = user.getDirection().scalarMultiply(0.4);
		if (user instanceof BendingPlayer) {
			boolean right = isMainHandRightSide((BendingPlayer) user);
			return right ? getRightSide(user).add(PLAYER_OFFSET).add(dir) : getLeftSide(user).add(PLAYER_OFFSET).add(dir);
		}
		return user.getEyeLocation().add(dir);
	}

	/**
	 * Gets the user's specified hand position.
	 * @param user the user to check
	 * @param right whether to get the right hand
	 * @return a vector which represents the user's specified hand location
	 */
	public static Vector3 getHandSide(@NonNull User user, boolean right) {
		Vector3 dir = user.getDirection().scalarMultiply(0.4);
		return right ? getRightSide(user).add(PLAYER_OFFSET).add(dir) : getLeftSide(user).add(PLAYER_OFFSET).add(dir);
	}

	/**
	 * Gets the user's right side.
	 * @param user the user to check
	 * @return a vector which represents the user's right side
	 */
	public static Vector3 getRightSide(@NonNull User user) {
		double angle = FastMath.toRadians(user.getYaw());
		return user.getLocation().subtract(new Vector3(FastMath.cos(angle), 0, FastMath.sin(angle)).normalize().scalarMultiply(0.3));
	}

	/**
	 * Gets the user's left side.
	 * @param user the user to check
	 * @return a vector which represents the user's left side
	 */
	public static Vector3 getLeftSide(@NonNull User user) {
		double angle = FastMath.toRadians(user.getYaw());
		return user.getLocation().add(new Vector3(FastMath.cos(angle), 0, FastMath.sin(angle)).normalize().scalarMultiply(0.3));
	}

	/**
	 * Checks the user's permissions and returns {@link Particle#FLAME} or {@link Particle#SOUL_FIRE_FLAME} accordingly.
	 * @param user the user to check
	 * @return th fire particle type
	 */
	public static Particle getFireParticles(@NonNull User user) {
		return user.hasPermission("bending.bluefire") ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
	}
}
