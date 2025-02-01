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

package me.moros.bending.api.util.data;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents a holder of data key-value pairs.
 */
public interface DataHolder {
  /**
   * Attempt to retrieve the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return the data that was retrieved if available
   */
  <T> Optional<T> get(DataKey<T> key);

  default OptionalInt getInt(DataKey<Integer> key) {
    return this.get(key).map(OptionalInt::of).orElseGet(OptionalInt::empty);
  }

  default OptionalDouble getDouble(DataKey<Double> key) {
    return this.get(key).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
  }

  default OptionalLong getLong(DataKey<Long> key) {
    return this.get(key).map(OptionalLong::of).orElseGet(OptionalLong::empty);
  }

  /**
   * Check whether this holder has the specified data key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return whether the key if available
   */
  default <T> boolean has(DataKey<T> key) {
    return get(key).isPresent();
  }

  /**
   * Store data for a specified key.
   * @param key the key used to store and access the data
   * @param value the data to store
   * @param <T> the type of data
   */
  <T> void add(DataKey<T> key, T value);

  /**
   * Attempt to remove the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   */
  <T> void remove(DataKey<T> key);
}
