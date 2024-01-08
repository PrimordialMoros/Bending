/*
 * Copyright 2020-2024 Moros
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

import java.util.Set;
import java.util.stream.Stream;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

/**
 * Represents a map-like container.
 * @param <V> the type of values for this container
 */
public interface Container<V> extends Keyed, Iterable<V> {
  /**
   * Check if the registry contains the specified value.
   * @param value the value to check
   * @return true if that value is registered, false otherwise
   */
  boolean containsValue(V value);

  /**
   * Check the size of this registry.
   * @return the amount of values this registry holds
   */
  int size();

  /**
   * Stream all the values contained in this registry.
   * @return a stream of this registry's values
   */
  Stream<V> stream();

  static <V> Container<V> create(Key key, Set<V> values) {
    return new ContainerImpl<>(key, Set.copyOf(values));
  }
}
