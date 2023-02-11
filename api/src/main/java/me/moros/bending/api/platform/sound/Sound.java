/*
 * Copyright 2020-2023 Moros
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

import me.moros.bending.api.registry.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.sound.Sound.Source;
import org.checkerframework.checker.nullness.qual.NonNull;

public sealed interface Sound extends Keyed, Sounds permits SoundImpl {
  @NonNull Key key();

  static Registry<Key, Sound> registry() {
    return SoundImpl.REGISTRY;
  }

  default SoundEffect asEffect() {
    return asEffect(1, 1);
  }

  default SoundEffect asEffect(float volume, float pitch) {
    return asEffect(Source.MASTER, volume, pitch);
  }

  default SoundEffect asEffect(Source source, float volume, float pitch) {
    return new SoundEffectImpl(key(), source, volume, pitch);
  }
}
