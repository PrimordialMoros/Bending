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

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.option.PositionSource;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.Nullable;

record ParticleOptionHolder(Map<ParticleOption<?>, Object> data) {
  private static final Map<Particle, ParticleOptionHolder> CACHE = new HashMap<>();

  @SuppressWarnings("unchecked")
  <V> @Nullable V get(ParticleOption<V> option) {
    return (V) data.get(option);
  }

  static ParticleOptionHolder.Builder emptyMutable() {
    return new Builder(new HashMap<>(6));
  }

  static ParticleOptionHolder immutableDefaults(Particle particle) {
    return CACHE.computeIfAbsent(particle, ParticleOptionHolder::generateDefaults);
  }

  record Builder(Map<ParticleOption<?>, Object> data) {
    <V> void put(ParticleOption<V> option, V value) throws IllegalArgumentException {
      String s = ((ParticleOptionImpl<V>) option).validateValue(value);
      if (s != null) {
        throw new IllegalArgumentException(s);
      }
      data.put(option, value);
    }

    ParticleOptionHolder build() {
      return new ParticleOptionHolder(Map.copyOf(data));
    }
  }

  private static ParticleOptionHolder generateDefaults(Particle particle) {
    Builder builder = emptyMutable();

    builder.put(ParticleOptions.OFFSET, Vector3d.ZERO);
    builder.put(ParticleOptions.QUANTITY, 1);
    builder.put(ParticleOptions.SPEED, 0D);

    if (particle == Particle.BLOCK ||
      particle == Particle.BLOCK_MARKER ||
      particle == Particle.FALLING_DUST ||
      particle == Particle.DUST_PILLAR ||
      particle == Particle.BLOCK_CRUMBLE) {
      builder.put(ParticleOptions.BLOCK_STATE, BlockType.AIR.defaultState());
    } else if (particle == Particle.DUST) {
      builder.put(ParticleOptions.COLOR, NamedTextColor.RED);
      builder.put(ParticleOptions.SCALE, 1D);
    } else if (particle == Particle.DUST_COLOR_TRANSITION) {
      builder.put(ParticleOptions.COLOR, NamedTextColor.RED);
      builder.put(ParticleOptions.TO_COLOR, NamedTextColor.RED);
      builder.put(ParticleOptions.SCALE, 1D);
    } else if (particle == Particle.ITEM) {
      builder.put(ParticleOptions.ITEM_SNAPSHOT, Platform.instance().factory().itemBuilder(Item.STONE).build());
    } else if (particle == Particle.SCULK_CHARGE) {
      builder.put(ParticleOptions.ROLL, 0D);
    } else if (particle == Particle.SHRIEK) {
      builder.put(ParticleOptions.DELAY, 0);
    } else if (particle == Particle.VIBRATION) {
      builder.put(ParticleOptions.DESTINATION, PositionSource.block(Vector3i.ZERO));
      builder.put(ParticleOptions.TRAVEL_TIME, 0);
    } else if (particle == Particle.ENTITY_EFFECT) {
      builder.put(ParticleOptions.COLOR, NamedTextColor.RED);
      builder.put(ParticleOptions.OPACITY, 1D);
    } else if (particle == Particle.TRAIL) {
      builder.put(ParticleOptions.TARGET, Vector3d.ZERO);
      builder.put(ParticleOptions.COLOR, NamedTextColor.RED);
      builder.put(ParticleOptions.TRAVEL_TIME, 0);
    }
    return builder.build();
  }
}
