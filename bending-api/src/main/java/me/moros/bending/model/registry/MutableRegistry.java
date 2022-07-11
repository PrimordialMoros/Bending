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

public interface MutableRegistry<K, V> extends Registry<K, V> {
  /**
   * Invalidates a key if it exists.
   * @param key the key to invalidate
   * @return true if key was invalidated, false otherwise
   */
  boolean invalidateKey(K key);

  /**
   * Invalidates a value if it exists.
   * @param value the value to invalidate
   * @return true if value was invalidated, false otherwise
   */
  boolean invalidateValue(V value);

  /**
   * Invalidates all given keys if they exist.
   * @param keys the keys to invalidate
   * @return the amount of keys that were invalidated
   */
  default int invalidateKeys(Iterable<K> keys) {
    int counter = 0;
    if (!isLocked()) {
      for (K key : keys) {
        if (invalidateKey(key)) {
          counter++;
        }
      }
    }
    return counter;
  }

  /**
   * Invalidates all given values if they exist.
   * @param values the values to invalidate
   * @return the amount of values that were invalidated
   */
  default int invalidateValues(Iterable<V> values) {
    int counter = 0;
    if (!isLocked()) {
      for (V value : values) {
        if (invalidateValue(value)) {
          counter++;
        }
      }
    }
    return counter;
  }

  /**
   * Clear all values from this registry.
   * @return true if any values were cleared, false otherwise
   */
  boolean clear();
}
