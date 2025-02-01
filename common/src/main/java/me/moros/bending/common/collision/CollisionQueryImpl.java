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

package me.moros.bending.common.collision;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

record CollisionQueryImpl<E>(Collection<Pair<E>> potentialCollisions) implements CollisionQuery<E> {
  CollisionQueryImpl() {
    this(new HashSet<>(32));
  }

  void add(E first, E second) {
    potentialCollisions.add(new SimplePair<>(first, second));
  }

  @Override
  public Iterator<Pair<E>> iterator() {
    return new Itr<>(potentialCollisions.iterator());
  }

  record SimplePair<E>(E first, E second) implements Pair<E> {
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      SimplePair<?> other = (SimplePair<?>) obj;
      return (first == other.first && second == other.second) || (first == other.second && second == other.first);
    }

    @Override
    public int hashCode() {
      int maxHash = Math.max(first.hashCode(), second.hashCode());
      int minHash = Math.min(first.hashCode(), second.hashCode());
      return 31 * minHash + maxHash;
    }
  }

  record Itr<E>(Iterator<E> iterator) implements Iterator<E> {
    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public E next() {
      return iterator.next();
    }
  }
}
