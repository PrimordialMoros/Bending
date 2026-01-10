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

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import net.kyori.adventure.key.Key;

record ContainerImpl<V>(Key key, Collection<V> view) implements Container<V> {
  @Override
  public boolean containsValue(V value) {
    return view().contains(value);
  }

  @Override
  public int size() {
    return view().size();
  }

  @Override
  public Stream<V> stream() {
    return view().stream();
  }

  @Override
  public Iterator<V> iterator() {
    return view().iterator();
  }
}
