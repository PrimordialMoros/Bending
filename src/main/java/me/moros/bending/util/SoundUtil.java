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

package me.moros.bending.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to provide a list of pre-configured sounds.
 * @see SoundEffect
 */
public final class SoundUtil {
  public static final SoundEffect AIR = new SoundEffect(Sound.ENTITY_CREEPER_HURT, 1, 2);

  public static final SoundEffect WATER = new SoundEffect(Sound.BLOCK_WATER_AMBIENT);
  public static final SoundEffect ICE = new SoundEffect(Sound.ITEM_FLINTANDSTEEL_USE);
  public static final SoundEffect PLANT = new SoundEffect(Sound.BLOCK_GRASS_STEP);

  public static final SoundEffect EARTH = new SoundEffect(Sound.ENTITY_GHAST_SHOOT);
  public static final SoundEffect SAND = new SoundEffect(Sound.BLOCK_SAND_BREAK);
  public static final SoundEffect METAL = new SoundEffect(Sound.ENTITY_IRON_GOLEM_HURT, 1, 1.25F);
  public static final SoundEffect LAVA = new SoundEffect(Sound.BLOCK_LAVA_AMBIENT);

  public static final SoundEffect FIRE = new SoundEffect(Sound.BLOCK_FIRE_AMBIENT);
  public static final SoundEffect COMBUSTION = new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
  public static final SoundEffect LIGHTNING = new SoundEffect(Sound.ENTITY_CREEPER_HURT, 1, 0);
  public static final SoundEffect LIGHTNING_BOLT = new SoundEffect(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);

  public static final SoundEffect FIRE_EXTINGUISH = new SoundEffect(Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 1);
  public static final SoundEffect LAVA_EXTINGUISH = new SoundEffect(Sound.BLOCK_LAVA_EXTINGUISH);

  public static final SoundEffect EXPLOSION = new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE);

  private SoundUtil() {
  }

  public static void playSound(@NonNull Location center, @NonNull Sound sound, float volume, float pitch) {
    center.getWorld().playSound(center, sound, volume, pitch);
  }
}
