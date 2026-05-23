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

import java.util.function.Function;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

record ParticleOptionImpl<V>(Key key, Class<? extends V> valueType,
                             @Nullable Function<V, String> validator) implements ParticleOption<V> {
  ParticleOptionImpl(Key key, Class<? extends V> valueType) {
    this(key, valueType, null);
  }

  public @Nullable String validateValue(V value) {
    return validator != null ? validator.apply(value) : null;
  }
}
