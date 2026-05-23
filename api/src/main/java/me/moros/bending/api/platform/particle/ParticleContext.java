/*
 * Copyright 2020-2026 Moros
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

import java.util.Optional;

import me.moros.math.Position;

public sealed interface ParticleContext permits ParticleContextImpl {
  Particle particle();

  Position position();

  default Position offset() {
    return option(ParticleOptions.OFFSET).orElseThrow();
  }

  default int count() {
    return option(ParticleOptions.QUANTITY).orElseThrow();
  }

  double extra();

  @Deprecated(forRemoval = true)
  Object data();

  <V> Optional<V> option(ParticleOption<V> option);
}
