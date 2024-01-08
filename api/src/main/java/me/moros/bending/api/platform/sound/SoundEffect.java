/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.platform.sound;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Position;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.sound.Sound.Source;

import static me.moros.bending.api.platform.sound.Sounds.*;

public interface SoundEffect extends Keyed {
  SoundEffect AIR = ENTITY_CREEPER_HURT.asEffect(1, 2);

  SoundEffect WATER = BLOCK_WATER_AMBIENT.asEffect();
  SoundEffect ICE = ITEM_FLINTANDSTEEL_USE.asEffect();
  SoundEffect PLANT = BLOCK_GRASS_STEP.asEffect();

  SoundEffect EARTH = ENTITY_GHAST_SHOOT.asEffect();
  SoundEffect SAND = BLOCK_SAND_BREAK.asEffect();
  SoundEffect METAL = ENTITY_IRON_GOLEM_HURT.asEffect(1, 1.25F);
  SoundEffect LAVA = BLOCK_LAVA_AMBIENT.asEffect();

  SoundEffect FIRE = BLOCK_FIRE_AMBIENT.asEffect();
  SoundEffect COMBUSTION = ENTITY_FIREWORK_ROCKET_BLAST.asEffect(1, 0);
  SoundEffect LIGHTNING = ENTITY_CREEPER_HURT.asEffect(1, 0);
  SoundEffect LIGHTNING_CHARGING = BLOCK_BEEHIVE_WORK.asEffect(2, 0.5F);

  SoundEffect FIRE_EXTINGUISH = BLOCK_FIRE_EXTINGUISH.asEffect(0.5F, 1);
  SoundEffect LAVA_EXTINGUISH = BLOCK_LAVA_EXTINGUISH.asEffect();

  SoundEffect EXPLOSION = ENTITY_GENERIC_EXPLODE.asEffect(2, 0);

  net.kyori.adventure.sound.Sound sound();

  default Key name() {
    return sound().name();
  }

  default Source source() {
    return sound().source();
  }

  default float volume() {
    return sound().volume();
  }

  default float pitch() {
    return sound().pitch();
  }

  /**
   * Play this sound effect at the center of the specified block.
   * @param block to block to play the sound at
   * @see #play(World, Position)
   */
  default void play(Block block) {
    block.world().playSound(sound(), block.x() + 0.5, block.y() + 0.5, block.z() + 0.5);
  }

  /**
   * Play this sound effect at the specified world and position.
   * @param world the world to play the sound in
   * @param center the position to play the sound at
   */
  default void play(World world, Position center) {
    world.playSound(sound(), center.x(), center.y(), center.z());
  }

  /**
   * Create a modified sound effect with the specified volume and pitch.
   * @param volume the new volume
   * @param pitch the new pitch
   * @return the new SoundEffect
   */
  default SoundEffect with(float volume, float pitch) {
    if (volume == volume() && pitch == pitch()) {
      return this;
    }
    return new SoundEffectImpl(name(), source(), volume, pitch);
  }
}
