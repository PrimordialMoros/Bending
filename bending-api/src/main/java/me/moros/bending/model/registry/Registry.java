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

import java.util.Set;
import java.util.stream.Stream;

import me.moros.bending.model.key.Namespaced;
import me.moros.bending.model.registry.exception.RegistryModificationException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common interface for bending registries.
 * @param <K> the type of keys for this registry
 * @param <V> the type of values for this registry
 */
public interface Registry<K, V> extends Namespaced, Iterable<V> {
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
   * Check if the registry contains a value for the specified key.
   * @param key the key to check
   * @return true if the key has an associated value, false otherwise
   */
  boolean containsKey(K key);

  /**
   * Check if the registry contains the specified value.
   * @param value the value to check
   * @return true if that value is registered, false otherwise
   */
  boolean containsValue(V value);

  /**
   * Get the value for the specified key.
   * @param key the key to check
   * @return the value associated with the given key or null if not found
   */
  @Nullable V get(K key);

  /**
   * Get the value for the specified key.
   * @param key the key to check
   * @return the value associated with the given key or null if not found
   */
  @Nullable V fromString(String key);

  /**
   * Check the size of this registry.
   * @return the amount of values this registry holds
   */
  int size();

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
   * Stream all the values contained in this registry.
   * @return a stream of this registry's values
   */
  Stream<V> stream();

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
}
