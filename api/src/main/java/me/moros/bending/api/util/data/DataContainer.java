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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import me.moros.bending.api.util.ExpiringSet;

/**
 * Represents a container for data.
 */
public interface DataContainer extends DataHolder {
  /**
   * Check if the data associated with the specified key can be edited.
   * When inserting/updating data, a cooldown can be used to prevent it from being changed too quickly.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return true if this object has a mapping for the specified key and its data can be edited, false otherwise
   */
  <T> boolean canEdit(DataKey<T> key);

  /**
   * Attempt to store data for a specified key.
   * @param key the key used to store and access the data
   * @param value the data to store
   * @param <T> the type of data
   * @return true if storing was successful, false otherwise
   */
  default <T> boolean offer(DataKey<T> key, T value) {
    if (canEdit(key)) {
      add(key, value);
      return true;
    }
    return false;
  }

  /**
   * A special operation for storing enum data that toggles the state of the enum.
   * Toggling the enum will choose the next possible value if possible or cycle to the beginning.
   * @param key the key associated with data
   * @param defaultValue the default value to store if no data exists
   * @param <T> the type of enum data
   * @return the resulting enum data after toggling
   */
  <T extends Enum<T>> T toggle(DataKey<T> key, T defaultValue);

  /**
   * Get whether this data container is empty.
   * @return true if no key-value mappings are present, false otherwise
   */
  boolean isEmpty();

  static DataContainer simple() {
    return new SimpleDataContainer(new ConcurrentHashMap<>());
  }

  static DataContainer blocking(long cooldown, TimeUnit unit) {
    return new BlockingDataContainer(new ConcurrentHashMap<>(), new ExpiringSet<>(cooldown, unit));
  }
}
