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

package me.moros.bending.api.platform.particle;

import me.moros.bending.api.registry.Registry;
import me.moros.math.Position;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public sealed interface Particle extends Keyed, Particles permits ParticleImpl {
  static Registry<Key, Particle> registry() {
    return ParticleImpl.REGISTRY;
  }

  /**
   * Create a new particle builder for this particle type.
   * @param pos the location to spawn the particles in
   * @return a new builder instance
   */
  default ParticleBuilder<Void> builder(Position pos) {
    return ParticleBuilder.of(this, pos);
  }

  /**
   * Create a new particle builder for this particle type.
   * @param pos the location to spawn the particles in
   * @return a new builder instance
   */
  default <T> ParticleBuilder<T> builder(T data, Position pos) {
    return ParticleBuilder.of(this, data, pos);
  }
}
