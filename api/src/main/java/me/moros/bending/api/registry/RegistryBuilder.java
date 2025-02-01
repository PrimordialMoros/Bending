/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.registry;

import java.util.Objects;
import java.util.function.Function;

import me.moros.bending.api.registry.SimpleRegistry.SimpleMutableRegistry;
import me.moros.bending.api.util.data.DataKey;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class for building registries.
 * @param <K> the key type
 * @param <V> the value type
 */
public final class RegistryBuilder<K, V> {
  private final DataKey<V> key;
  private final Function<V, K> inverseMapper;
  private final Function<String, @Nullable K> keyMapper;

  private RegistryBuilder(DataKey<V> key, Function<V, K> inverseMapper, Function<String, @Nullable K> keyMapper) {
    this.key = key;
    this.inverseMapper = inverseMapper;
    this.keyMapper = keyMapper;
  }

  public Registry<K, V> build() {
    return new SimpleRegistry<>(key, inverseMapper, keyMapper);
  }

  public MutableRegistry<K, V> buildMutable() {
    return new SimpleMutableRegistry<>(key, inverseMapper, keyMapper);
  }

  public DefaultedRegistry<K, V> buildDefaulted(Function<K, V> factory) {
    Objects.requireNonNull(factory);
    return new DefaultedRegistry<>(key, inverseMapper, keyMapper, factory);
  }

  public static final class IntermediaryRegistryBuilder<V> {
    private final DataKey<V> key;

    IntermediaryRegistryBuilder(DataKey<V> key) {
      this.key = key;
    }

    public <K> IntermediaryRegistryBuilder2<K, V> inverseMapper(Function<V, K> inverseMapper) {
      Objects.requireNonNull(inverseMapper);
      return new IntermediaryRegistryBuilder2<>(key, inverseMapper);
    }
  }

  public static final class IntermediaryRegistryBuilder2<K, V> {
    private final DataKey<V> key;
    private final Function<V, K> inverseMapper;

    IntermediaryRegistryBuilder2(DataKey<V> key, Function<V, K> inverseMapper) {
      this.key = key;
      this.inverseMapper = inverseMapper;
    }

    public RegistryBuilder<K, V> keyMapper(Function<String, @Nullable K> keyMapper) {
      Objects.requireNonNull(keyMapper);
      return new RegistryBuilder<>(key, inverseMapper, keyMapper);
    }
  }
}
