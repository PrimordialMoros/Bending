/*
 * Copyright 2020-2026 Moros
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

import java.util.function.Function;
import java.util.stream.Stream;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Represents a holder of tags.
 * @param <V> the type of value the tag contains
 */
public interface TagHolder<V> {
  /**
   * Get the tag for the specified key.
   * @param key the key to check
   * @return the tag associated with the given key or null if not found
   */
  @Nullable Tag<V> getTag(Key key);

  /**
   * Get the tag for the specified key or create one if it doesn't exist.
   * @param key the key to check
   * @param factory the function to create a tag
   * @return the tag associated with the given key
   */
  Tag<V> getTagOrCreate(Key key, Function<Key, Tag<V>> factory);

  /**
   * Registers a tag of values if it doesn't exist.
   * @param tag the tag to register
   * @return true if the tag was registered, false otherwise
   * @throws RegistryModificationException if registry is locked
   */
  boolean registerTag(Tag<V> tag);

  /**
   * Stream all the tags in this registry.
   * @return a stream of this registry's tags
   */
  Stream<Tag<V>> tags();
}
