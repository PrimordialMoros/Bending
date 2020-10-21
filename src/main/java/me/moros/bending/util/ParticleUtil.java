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

package me.moros.bending.util;

import com.destroystokyo.paper.ParticleBuilder;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.UserMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Utility class to provide create and render {@link Particle}.
 * You should prefer this over using a custom ParticleBuilder to ensure uniform rendering across different abilities.
 * @see ParticleBuilder
 */
public final class ParticleUtil {
	public static final int DEFAULT_DIST = 32;
	public static final Color AIR = fromHex("EEEEEE");

	public static ParticleBuilder createFire(@NonNull User user, @NonNull Location center) {
		return UserMethods.getFireParticles(user).builder().location(center).receivers(DEFAULT_DIST).extra(0).force(true);
	}

	public static ParticleBuilder createAir(@NonNull Location center) {
		return Particle.REDSTONE.builder().location(center).receivers(DEFAULT_DIST).extra(0).color(AIR, 1.8f).force(true);
	}

	public static ParticleBuilder createRGB(@NonNull Location center, @NonNull String hexVal) {
		return Particle.REDSTONE.builder().location(center).receivers(DEFAULT_DIST).extra(0).color(fromHex(hexVal)).force(true);
	}

	public static ParticleBuilder create(@NonNull Particle effect, @NonNull Location center) {
		return effect.builder().location(center).receivers(DEFAULT_DIST).extra(0).force(true);
	}

	public static ParticleBuilder create(@NonNull Particle effect, @NonNull Location center, int range) {
		return effect.builder().location(center).receivers(range).extra(0).force(true);
	}

	/**
	 * Asynchronously spawns and sends the given particle effect to its receivers.
	 * Make sure you have collected the receivers in a sync thread first.
	 * @param pb the particle effect builder that holds the particle data to display
	 */
	public static void displayAsync(@NonNull ParticleBuilder pb) {
		if (pb.hasReceivers()) Tasker.newChain().async(pb::spawn).execute();
	}

	/**
	 * Convert a hex string into a {@link Color}.
	 * @param hexValue the string holding the hex value, needs to be in the format "RRGGBB"
	 * @return the color from the provided hex value or {@link Color#BLACK} if hex value was invalid
	 */
	public static Color fromHex(@NonNull String hexValue) {
		if (hexValue.length() < 6) return Color.BLACK;
		int r = Integer.valueOf(hexValue.substring(0, 2), 16);
		int g = Integer.valueOf(hexValue.substring(2, 4), 16);
		int b = Integer.valueOf(hexValue.substring(4, 6), 16);
		return Color.fromRGB(r, g, b);
	}
}
