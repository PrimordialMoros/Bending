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

package me.moros.bending.model.user;

import me.moros.bending.model.BendingKey;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an object that holds a map collection of data.
 */
public interface DataContainer {
  /**
   * Check if this object contains data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return true if this object contains a mapping for the specified key
   */
  <T> boolean containsKey(BendingKey<T> key);

  /**
   * Check if the data associated with the specified key can be edited.
   * When inserting/updating data, a cooldown can be used to prevent it from being changed too quickly.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return true if this object has a mapping for the specified key and its data can be edited, false otherwise
   */
  <T> boolean canEdit(BendingKey<T> key);

  /**
   * Attempt to store data for a specified key.
   * @param key the key used to store and access the data
   * @param value the data to store
   * @param <T> the type of data
   * @return true if storing was successful, false otherwise
   */
  default <T> boolean offer(BendingKey<T> key, T value) {
    if (canEdit(key)) {
      put(key, value);
      return true;
    }
    return false;
  }

  /**
   * Store data for a specified key, ignoring cooldowns.
   * @param key the key used to store and access the data
   * @param value the data to store
   * @param <T> the type of data
   */
  <T> void put(BendingKey<T> key, T value);

  /**
   * Attempt to remove the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return the data that was removed or null if no data was associated with the given key
   */
  <T> @Nullable T remove(BendingKey<T> key);

  /**
   * Attempt to retrieve the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return the data that was retrieved or null if no data was associated with the given key
   */
  <T> @Nullable T get(BendingKey<T> key);

  /**
   * Attempt to retrieve the data for the specified key.
   * @param key the key associated with data
   * @param defaultValue the default value to return if no data was found
   * @param <T> the type of data
   * @return the result
   */
  default <T> T getOrDefault(BendingKey<T> key, T defaultValue) {
    T oldValue = get(key);
    return oldValue != null ? oldValue : defaultValue;
  }

  /**
   * A special operation for storing enum data that toggles the state of the enum.
   * Toggling the enum will choose the next possible value if possible or cycle to the beginning.
   * @param key the key associated with data
   * @param defaultValue the default value to store if no data exists
   * @param <T> the type of enum data
   * @return the resulting enum data after toggling
   */
  <T extends Enum<T>> T toggle(BendingKey<T> key, T defaultValue);
}
