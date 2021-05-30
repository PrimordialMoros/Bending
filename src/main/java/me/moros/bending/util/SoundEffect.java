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

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to provide a {@link Sound} along with volume and pitch.
 * @see SoundUtil
 */
public class SoundEffect {
  private final Sound sound;
  private final float volume;
  private final float pitch;

  public SoundEffect(@NonNull Sound sound) {
    this(sound, 1, 1);
  }

  public SoundEffect(@NonNull Sound sound, float volume, float pitch) {
    this.sound = Objects.requireNonNull(sound);
    this.volume = volume;
    this.pitch = pitch;
  }

  public @NonNull Sound sound() {
    return sound;
  }

  public float volume() {
    return volume;
  }

  public float pitch() {
    return pitch;
  }

  public void play(@NonNull Location center) {
    play(center, volume, pitch);
  }

  public void play(@NonNull Location center, float v, float p) {
    SoundUtil.playSound(center, sound, v, p);
  }
}
