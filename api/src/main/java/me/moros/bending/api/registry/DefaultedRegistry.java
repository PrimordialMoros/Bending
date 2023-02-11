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

package me.moros.bending.api.registry;

import java.util.function.Function;

public class DefaultedRegistry<K, V> extends SimpleRegistry<K, V> {
  private final Function<K, V> factory;

  protected DefaultedRegistry(String namespace, Function<V, K> inverseMapper, Function<String, K> keyMapper, Function<K, V> factory) {
    super(namespace, inverseMapper, keyMapper);
    this.factory = factory;
  }

  @Override
  public V get(K key) {
    var result = super.get(key);
    if (result == null) {
      result = factory.apply(key);
      register(result);
    }
    return result;
  }

  @Override
  public void lock() {
    throw new UnsupportedOperationException("Cannot lock defaulted registry");
  }

  public void clear() {
    checkLock();
    tags.clear();
    registryMap.clear();
  }
}
