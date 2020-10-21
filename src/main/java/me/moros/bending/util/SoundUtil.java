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

import org.bukkit.Location;
import org.bukkit.Sound;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to provide a list of pre-configured sounds.
 * @see SoundEffect
 */
public final class SoundUtil {
	public static final SoundEffect AIR_SOUND = new SoundEffect(Sound.ENTITY_CREEPER_HURT, 1, 2);

	public static final SoundEffect WATER_SOUND = new SoundEffect(Sound.BLOCK_WATER_AMBIENT);
	public static final SoundEffect ICE_SOUND = new SoundEffect(Sound.ITEM_FLINTANDSTEEL_USE);
	public static final SoundEffect PLANT_SOUND = new SoundEffect(Sound.BLOCK_GRASS_STEP);

	public static final SoundEffect EARTH_SOUND = new SoundEffect(Sound.ENTITY_GHAST_SHOOT);
	public static final SoundEffect SAND_SOUND = new SoundEffect(Sound.BLOCK_SAND_BREAK);
	public static final SoundEffect METAL_SOUND = new SoundEffect(Sound.ENTITY_IRON_GOLEM_HURT, 1, 1.25f);
	public static final SoundEffect LAVA_SOUND = new SoundEffect(Sound.BLOCK_LAVA_AMBIENT);

	public static final SoundEffect FIRE_SOUND = new SoundEffect(Sound.BLOCK_FIRE_AMBIENT);
	public static final SoundEffect COMBUSTION_SOUND = new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
	public static final SoundEffect LIGHTNING_SOUND = new SoundEffect(Sound.ENTITY_CREEPER_HURT, 1, 0);

	public static void playSound(@NonNull Location center, @NonNull SoundEffect effect, float volume, float pitch) {
		playSound(center, effect.getSound(), volume, pitch);
	}

	public static void playSound(@NonNull Location center, @NonNull Sound sound) {
		playSound(center, sound, 1, 1);
	}

	public static void playSound(@NonNull Location center, @NonNull Sound sound, float volume, float pitch) {
		center.getWorld().playSound(center, sound, volume, pitch);
	}
}
