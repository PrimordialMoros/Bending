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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.sound.Sound.Type;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.Sound.*;

/**
 * Utility class to provide a list of pre-configured sounds.
 */
public final class SoundUtil {
  public static final SoundEffect AIR = of(ENTITY_CREEPER_HURT, 1, 2);

  public static final SoundEffect WATER = of(BLOCK_WATER_AMBIENT);
  public static final SoundEffect ICE = of(ITEM_FLINTANDSTEEL_USE);
  public static final SoundEffect PLANT = of(BLOCK_GRASS_STEP);

  public static final SoundEffect EARTH = of(ENTITY_GHAST_SHOOT);
  public static final SoundEffect SAND = of(BLOCK_SAND_BREAK);
  public static final SoundEffect METAL = of(ENTITY_IRON_GOLEM_HURT, 1, 1.25F);
  public static final SoundEffect LAVA = of(BLOCK_LAVA_AMBIENT);

  public static final SoundEffect FIRE = of(BLOCK_FIRE_AMBIENT);
  public static final SoundEffect COMBUSTION = of(ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
  public static final SoundEffect LIGHTNING = of(ENTITY_CREEPER_HURT, 1, 0);
  public static final SoundEffect LIGHTNING_CHARGING = of(BLOCK_BEEHIVE_WORK, 2, 0.5F);

  public static final SoundEffect FIRE_EXTINGUISH = of(BLOCK_FIRE_EXTINGUISH, 0.5F, 1);
  public static final SoundEffect LAVA_EXTINGUISH = of(BLOCK_LAVA_EXTINGUISH);

  public static final SoundEffect EXPLOSION = explosion(2, 0);

  private SoundUtil() {
  }

  public static void playSound(@NonNull Location center, @NonNull Sound sound) {
    center.getWorld().playSound(sound, center.getX(), center.getY(), center.getZ());
  }

  public static void playSound(@NonNull Location center, @NonNull Sound sound, float volume, float pitch) {
    playSound(center, Sound.sound(sound.name(), sound.source(), volume, pitch));
  }

  public static void playSound(@NonNull Location center, Type sound, float volume, float pitch) {
    of(sound, volume, pitch).play(center);
  }

  private static SoundEffect of(Type sound) {
    return of(sound, 1, 1);
  }

  private static SoundEffect of(Type sound, float volume, float pitch) {
    return new SoundEffect(Sound.sound(sound, Source.MASTER, volume, pitch));
  }

  public static @NonNull SoundEffect explosion(float volume, float pitch) {
    return of(ENTITY_GENERIC_EXPLODE, volume, pitch);
  }

  /**
   * Wrapper for {@link Sound} with utility methods.
   * @see SoundUtil
   */
  public static class SoundEffect implements Sound.Type {
    private final Sound sound;

    private SoundEffect(Sound sound) {
      this.sound = sound;
    }

    public void play(@NonNull Location center) {
      playSound(center, sound);
    }

    public void play(@NonNull Location center, float volume, float pitch) {
      playSound(center, sound, volume, pitch);
    }

    @Override
    public @NotNull Key key() {
      return sound.name();
    }
  }
}
