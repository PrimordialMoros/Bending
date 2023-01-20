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

package me.moros.bending.model.registry;

import java.util.Objects;
import java.util.function.Function;

import me.moros.bending.model.registry.SimpleRegistry.SimpleMutableRegistry;

/**
 * Utility class for building registries.
 * @param <K> the key type
 * @param <V> the value type
 */
public final class RegistryBuilder<K, V> {
  private final String namespace;
  private final Function<V, K> inverseMapper;
  private Function<String, K> keyMapper;

  private RegistryBuilder(String namespace, Function<V, K> inverseMapper) {
    this.namespace = namespace;
    this.inverseMapper = inverseMapper;
  }

  public RegistryBuilder<K, V> keyMapper(Function<String, K> keyMapper) {
    this.keyMapper = Objects.requireNonNull(keyMapper);
    return this;
  }

  public Registry<K, V> build() {
    return new SimpleRegistry<>(namespace, inverseMapper, keyMapper);
  }

  public MutableRegistry<K, V> buildMutable() {
    return new SimpleMutableRegistry<>(namespace, inverseMapper, keyMapper);
  }

  public DefaultedRegistry<K, V> buildDefaulted(Function<K, V> factory) {
    Objects.requireNonNull(factory);
    return new DefaultedRegistry<>(namespace, inverseMapper, keyMapper, factory);
  }

  public static class IntermediaryRegistryBuilder<V> {
    private final String namespace;

    IntermediaryRegistryBuilder(String namespace) {
      this.namespace = namespace;
    }

    public <K1> RegistryBuilder<K1, V> inverseMapper(Function<V, K1> inverseMapper) {
      return new RegistryBuilder<>(namespace, Objects.requireNonNull(inverseMapper));
    }
  }
}
