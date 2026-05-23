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
import me.moros.bending.api.platform.particle.ParticleOptions;
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

  default ParticleBuilder asParticle(Position center) {
    return Particle.BLOCK.builder(center).option(ParticleOptions.BLOCK_STATE, this);
  }

  default ParticleBuilder asFallingParticle(Position center) {
    return Particle.FALLING_DUST.builder(center).option(ParticleOptions.BLOCK_STATE, this);
  }

  default ParticleBuilder asMarkerParticle(Position center) {
    return Particle.BLOCK_MARKER.builder(center).option(ParticleOptions.BLOCK_STATE, this);
  }

  default ParticleBuilder asDustPilarParticle(Position center) {
    return Particle.DUST_PILLAR.builder(center).option(ParticleOptions.BLOCK_STATE, this);
  }

  default ParticleBuilder asBlockCrumbleParticle(Position center) {
    return Particle.BLOCK_CRUMBLE.builder(center).option(ParticleOptions.BLOCK_STATE, this);
  }

  <V extends Comparable<V>> BlockState withProperty(Property<V> property, V value);
}
