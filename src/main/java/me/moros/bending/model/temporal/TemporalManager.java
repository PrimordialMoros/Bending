/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.temporal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;

public class TemporalManager<K, V extends Temporary> {
  private final Map<K, V> instances;

  public TemporalManager() {
    instances = new ConcurrentHashMap<>();
  }

  public boolean isTemp(@Nullable K key) {
    if (key == null) {
      return false;
    }
    return instances.containsKey(key);
  }

  public Optional<V> get(@NonNull K key) {
    return Optional.ofNullable(instances.get(key));
  }

  public void addEntry(@NonNull K key, @NonNull V value) {
    if (isTemp(key)) {
      return;
    }
    instances.put(key, value);
  }

  /**
   * This is used inside {@link Temporary#revert}
   * @param key the key of the entry to remove
   */
  public void removeEntry(@NonNull K key) {
    instances.remove(key);
  }

  public void removeAll() {
    new ArrayList<>(instances.values()).forEach(Temporary::revert);
    clear();
  }

  protected @NonNull Map<K, V> getInstances() {
    return Collections.unmodifiableMap(instances);
  }

  protected void clear() {
    instances.clear();
  }
}
