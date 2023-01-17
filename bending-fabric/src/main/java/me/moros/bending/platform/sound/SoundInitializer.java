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

package me.moros.bending.platform.sound;

import me.moros.bending.platform.Initializer;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;

public final class SoundInitializer implements Initializer {
  @Override
  public void init() {
    for (var sound : BuiltInRegistries.SOUND_EVENT) {
      Key key = sound.getLocation();
      if (!Sound.registry().containsKey(key)) {
        Sound.registry().register(new SoundImpl(key));
      }
    }
  }
}
