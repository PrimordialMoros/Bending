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

package me.moros.bending.api.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Represents a set with expiring elements based on a {@link Caffeine} cache.
 * @param <E> the type of elements
 */
public class ExpiringSet<E> {
  private final Cache<E, Boolean> cache;

  /**
   * Create a new expiring set.
   * @param duration the duration of the set elements in milliseconds
   */
  public ExpiringSet(long duration) {
    this(duration, TimeUnit.MILLISECONDS);
  }

  /**
   * Create a new expiring set.
   * @param duration the duration of the set elements
   * @param unit the time unit
   */
  public ExpiringSet(long duration, TimeUnit unit) {
    cache = Caffeine.newBuilder().expireAfterWrite(duration, unit).build();
  }

  /**
   * Temporarily add the given element to the expiring set.
   * @param item the element to add
   */
  public void forceAdd(E item) {
    cache.put(item, false);
  }

  /**
   * Temporarily add the given element to the expiring set.
   * @param item the element to add
   * @return true if the element was added as a result of this call, false otherwise
   */
  public boolean add(E item) {
    if (contains(item)) {
      return false;
    }
    forceAdd(item);
    return true;
  }

  /**
   * Check if the specified element is contained in this expiring set.
   * @param item the element to check
   * @return true if this set currently contains the given element, false otherwise
   */
  public boolean contains(E item) {
    return cache.getIfPresent(item) != null;
  }

  /**
   * Create an immutable snapshot of this expiring set.
   * @return the snapshot set
   */
  public Set<E> snapshot() {
    return Set.copyOf(cache.asMap().keySet());
  }
}
