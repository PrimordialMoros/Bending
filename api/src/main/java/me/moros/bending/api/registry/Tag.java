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

import java.util.Locale;

import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;

/**
 * Represents a container of elements that share a common characteristic.
 * Tags are primarily used in vanilla registries.
 * @param <V> the type of elements
 */
public interface Tag<V> extends Container<V> {
  /**
   * Check if this tag contains an element associated with the specified key.
   * @param key the key to check
   * @return true if the tag contains the element, false otherwise
   */
  boolean isTagged(Key key);

  /**
   * Check if this tag contains an element associated with the given string key.
   * @param value the string key to check
   * @return true if the tag contains the element, false otherwise
   */
  default boolean isTagged(String value) {
    Key key = KeyUtil.VANILLA_KEY_MAPPER.apply(value.toLowerCase(Locale.ROOT));
    return key != null && isTagged(key);
  }

  /**
   * Check if this tag contains the specified element.
   * @param value the value to check
   * @return true if the tag contains the element, false otherwise
   */
  default boolean isTagged(V value) {
    return containsValue(value);
  }
}
