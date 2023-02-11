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

package me.moros.bending.api.registry;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import me.moros.bending.api.registry.RegistryBuilder.IntermediaryRegistryBuilder;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common interface for bending registries.
 * @param <K> the type of keys for this registry
 * @param <V> the type of values for this registry
 */
public interface Registry<K, V> extends Container<V>, TagHolder<V> {
  /**
   * Check if the registry contains a value for the specified key.
   * @param key the key to check
   * @return true if the key has an associated value, false otherwise
   */
  boolean containsKey(K key);

  /**
   * Get the value for the specified key.
   * @param key the key to check
   * @return the value associated with the given key or null if not found
   */
  @Nullable V get(K key);

  /**
   * Get the value for the specified key.
   * @param key the key to check
   * @return the value associated with the given key
   * @throws NullPointerException if registry mapping is not found
   */
  default V getOrThrow(K key) {
    return Objects.requireNonNull(get(key), "No mapping found for " + key);
  }

  /**
   * Get the value for the specified key.
   * @param key the key to check
   * @return the value associated with the given key or def if not found
   */
  default Optional<V> getIfExists(K key) {
    return Optional.ofNullable(get(key));
  }

  /**
   * Get the value for the specified key.
   * @param input the key to check
   * @return the value associated with the given key or null if not found
   */
  @Nullable V fromString(String input);

  /**
   * Registers a value if it doesn't exist.
   * @param value the value to register
   * @return true if value was registered, false otherwise
   * @throws RegistryModificationException if registry is locked
   */
  boolean register(V value);

  /**
   * Registers all given values if they don't exist.
   * @param values the values to register
   * @return the amount of values that were registered
   */
  default int register(Iterable<V> values) {
    int counter = 0;
    if (!isLocked()) {
      for (V value : values) {
        if (register(value)) {
          counter++;
        }
      }
    }
    return counter;
  }

  /**
   * Lock this registry to prevent further modifications.
   */
  void lock();

  /**
   * Check if this registry is locked.
   * @return true if locked, false otherwise
   */
  boolean isLocked();

  /**
   * Stream all the keys in this registry.
   * @return a stream of this registry's keys
   */
  Stream<K> streamKeys();

  /**
   * A collection of all currently registered keys.
   * @return an immutable collection of this registry's keys
   */
  Set<K> keys();

  static <V> IntermediaryRegistryBuilder<V> builder(String namespace) {
    return new IntermediaryRegistryBuilder<>(namespace);
  }

  static <V extends Keyed> RegistryBuilder<Key, V> simpleBuilder(String namespace) {
    return Registry.<V>builder(namespace).inverseMapper(Keyed::key).keyMapper(KeyUtil.stringToKey(namespace));
  }

  static <V extends Keyed> RegistryBuilder<Key, V> vanillaBuilder(String namespace) {
    return Registry.<V>builder(namespace).inverseMapper(Keyed::key).keyMapper(KeyUtil.VANILLA_KEY_MAPPER);
  }

  static <V extends Keyed> Registry<Key, V> vanilla(String namespace) {
    return Registry.<V>vanillaBuilder(namespace).build();
  }

  static <V extends Keyed> DefaultedRegistry<Key, V> vanillaDefaulted(String namespace, Function<Key, V> factory) {
    return Registry.<V>vanillaBuilder(namespace).buildDefaulted(factory);
  }
}
