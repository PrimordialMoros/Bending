/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import me.moros.math.Vector3d;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.sound.Sound.Type;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

import static org.bukkit.Sound.*;

/**
 * Utility class that also provides a list of pre-configured sounds.
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

  public static final SoundEffect EXPLOSION = of(ENTITY_GENERIC_EXPLODE, 2, 0);

  private SoundUtil() {
  }

  /**
   * Create a new sound effect of the specified type with default volume and pitch.
   * @param sound the sound type to use
   * @return the new SoundEffect
   * @see #of(Type, float, float)
   */
  public static SoundEffect of(Type sound) {
    return of(sound, 1, 1);
  }

  /**
   * Create a new sound effect of the specified type, volume and pitch.
   * @param sound the sound type to use
   * @param volume the volume to use
   * @param pitch the pitch to use
   * @return the new SoundEffect
   */
  public static SoundEffect of(Type sound, float volume, float pitch) {
    return new SoundEffect(Sound.sound(sound, Source.MASTER, volume, pitch));
  }

  /**
   * Wrapper for {@link Sound} with utility methods.
   * @see SoundUtil
   */
  public static final class SoundEffect implements Sound.Type {
    private final Sound sound;

    private SoundEffect(Sound sound) {
      this.sound = sound;
    }

    /**
     * Play this sound effect at the center of the specified block.
     * @param block to block to play the sound at
     * @see #play(World, Vector3d)
     */
    public void play(Block block) {
      block.getWorld().playSound(sound, block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }

    /**
     * Play this sound effect at the specified world and position.
     * @param world the world to play the sound in
     * @param center the center to play the sound at
     */
    public void play(World world, Vector3d center) {
      world.playSound(sound, center.x(), center.y(), center.z());
    }

    /**
     * Create a modified sound effect with the specified volume and pitch.
     * @param volume the new volume
     * @param pitch the new pitch
     * @return the new SoundEffect
     */
    public SoundEffect with(float volume, float pitch) {
      if (volume == sound.volume() && pitch == sound.pitch()) {
        return this;
      }
      return new SoundEffect(Sound.sound(sound.name(), sound.source(), volume, pitch));
    }

    @Override
    public @NonNull Key key() {
      return sound.name();
    }
  }
}
