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

package me.moros.bending.model.user;

import me.moros.bending.model.key.RegistryKey;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an object that holds a map collection of data.
 */
public interface DataHolder {
  /**
   * Check if this object contains data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return true if this object contains a mapping for the specified key
   */
  <T> boolean containsKey(RegistryKey<T> key);

  /**
   * Check if the data associated with the specified key can be edited.
   * When inserting/updating data, a cooldown can be used to prevent it from being changed too quickly.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return true if this object has a mapping for the specified key and its data can be edited, false otherwise
   */
  <T> boolean canEdit(RegistryKey<T> key);

  /**
   * Attempt to store data for a specified key.
   * @param key the key used to store and access the data
   * @param value the data to store
   * @param <T> the type of data
   * @return true if storing was successful, false otherwise
   */
  <T> boolean offer(RegistryKey<T> key, T value);

  /**
   * Attempt to remove the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return the data that was removed or null if no data was associated with the given key
   */
  <T> @Nullable T remove(RegistryKey<T> key);

  /**
   * Attempt to retrieve the data for the specified key.
   * @param key the key associated with data
   * @param <T> the type of data
   * @return the data that was retrieved or null if no data was associated with the given key
   */
  <T> @Nullable T get(RegistryKey<T> key);

  /**
   * Attempt to retrieve the data for the specified key.
   * @param key the key associated with data
   * @param defaultValue the default value to return if no data was found
   * @param <T> the type of data
   * @return the result
   */
  <T> T getOrDefault(RegistryKey<T> key, T defaultValue);

  /**
   * A special operation for storing enum data that toggles the state of the enum.
   * Toggling the enum will choose the next possible value if possible or cycle to the beginning.
   * @param key the key associated with data
   * @param defaultValue the default value to store if no data exists
   * @param <T> the type of enum data
   * @return the resulting enum data after toggling
   */
  <T extends Enum<T>> T toggle(RegistryKey<T> key, T defaultValue);
}
