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
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.UserMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class ParticleUtil {
	public static final int DEFAULT_DIST = 32;
	public static final Color AIR = fromHex("EEEEEE");

	public static ParticleBuilder createFire(User user, Location center) {
		return UserMethods.getFireParticles(user).builder().location(center).receivers(DEFAULT_DIST).extra(0);
	}

	public static ParticleBuilder createAir(Location center) {
		return Particle.REDSTONE.builder().location(center).receivers(DEFAULT_DIST).extra(0).color(AIR);
	}

	public static ParticleBuilder createRGB(Location center, String hexVal) {
		return Particle.REDSTONE.builder().location(center).receivers(DEFAULT_DIST).extra(0).color(fromHex(hexVal));
	}

	public static ParticleBuilder create(Particle effect, Location center) {
		return effect.builder().location(center).receivers(DEFAULT_DIST).extra(0);
	}

	public static ParticleBuilder create(Particle effect, Location center, int range) {
		return effect.builder().location(center).receivers(Math.min(range, DEFAULT_DIST)).extra(0);
	}

	/**
	 * Asynchronously spawns and sends the given particle effect to its receivers.
	 *
	 * @param pb the particle effect builder that holds the particle data to display
	 */
	public static void displayAsync(ParticleBuilder pb) {
		if (pb.hasReceivers()) Tasker.newChain().async(pb::spawn).execute();
	}

	public static Color fromHex(String hexVal) {
		if (hexVal.length() < 6) return Color.BLACK;
		int r = Integer.valueOf(hexVal.substring(0, 2), 16);
		int g = Integer.valueOf(hexVal.substring(2, 4), 16);
		int b = Integer.valueOf(hexVal.substring(4, 6), 16);
		return Color.fromRGB(r, g, b);
	}
}
