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

package me.moros.bending.api.platform.block;

import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.property.Property;
import me.moros.bending.api.platform.property.PropertyHolder;
import me.moros.math.Position;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public interface BlockState extends Keyed, PropertyHolder {
  @Override
  default Key key() {
    return type().key();
  }

  BlockType type();

  boolean matches(BlockState other);

  default ParticleBuilder<BlockState> asParticle(Position center) {
    return Particle.BLOCK.builder(this, center);
  }

  default ParticleBuilder<BlockState> asFallingParticle(Position center) {
    return Particle.FALLING_DUST.builder(this, center);
  }

  default ParticleBuilder<BlockState> asMarkerParticle(Position center) {
    return Particle.BLOCK_MARKER.builder(this, center);
  }

  <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value);
}
