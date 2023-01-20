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

package me.moros.bending.platform.block;

import java.util.Optional;

import me.moros.bending.model.registry.Registry;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.sound.SoundGroup;
import me.moros.math.Position;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public sealed interface BlockType extends Keyed, BlockProperties, BlockTypes permits BlockTypeImpl {
  static Registry<Key, BlockType> registry() {
    return BlockTypeImpl.REGISTRY;
  }

  default String name() {
    return key().value();
  }

  SoundGroup soundGroup();

  BlockState defaultState();

  Optional<Item> asItem();

  default ParticleBuilder<BlockState> asParticle(Position center) {
    return defaultState().asParticle(center);
  }

  default ParticleBuilder<BlockState> asFallingParticle(Position center) {
    return defaultState().asFallingParticle(center);
  }

  default ParticleBuilder<BlockState> asMarkerParticle(Position center) {
    return defaultState().asMarkerParticle(center);
  }
}
