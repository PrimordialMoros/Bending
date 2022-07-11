/*
 * Copyright 2020-2022 Moros
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

import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.registry.SimpleRegistry.SimpleMutableRegistry;

public class RegistryBuilder<K, V> {
  protected final String namespace;

  private RegistryBuilder(String namespace) {
    this.namespace = namespace;
  }

  private RegistryBuilder(RegistryBuilder<K, V> builder) {
    this(builder.namespace);
  }

  public <K1> RegistryBuilder1<K1, V> inverseMapper(Function<V, K1> inverseMapper) {
    RegistryBuilder1<K1, V> newBuilder = new RegistryBuilder1<>(namespace);
    newBuilder.inverseMapper = Objects.requireNonNull(inverseMapper);
    return newBuilder;
  }

  public static final class RegistryBuilder1<K, V> extends RegistryBuilder<K, V> {
    Function<V, K> inverseMapper;
    Function<String, K> keyMapper;

    private RegistryBuilder1(String namespace) {
      super(namespace);
    }

    private RegistryBuilder1(RegistryBuilder1<K, V> builder) {
      super(builder);
      this.inverseMapper = builder.inverseMapper;
      this.keyMapper = builder.keyMapper;
    }

    public RegistryBuilder1<K, V> keyMapper(Function<String, K> keyMapper) {
      this.keyMapper = Objects.requireNonNull(keyMapper);
      return this;
    }

    public Registry<K, V> build() {
      return new SimpleRegistry<>(namespace, inverseMapper, keyMapper);
    }

    public MutableRegistry<K, V> buildMutable() {
      return new SimpleMutableRegistry<>(namespace, inverseMapper, keyMapper);
    }
  }

  public static <V> RegistryBuilder<?, V> builder(RegistryKey<V> type) {
    return new RegistryBuilder<>(type.namespace());
  }
}
