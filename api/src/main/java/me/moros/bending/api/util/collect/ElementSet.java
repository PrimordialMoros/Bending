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

package me.moros.bending.api.util.collect;

import java.util.Set;

import me.moros.bending.api.ability.element.Element;

/**
 * A thread-safe set of elements using atomic operations and a tiny footprint.
 */
public sealed interface ElementSet extends Set<Element> permits MutableElementSet, ImmutableElementSet {
  int elements();

  boolean set(Set<Element> other);

  static ElementSet mutable() {
    return new MutableElementSet();
  }

  static ElementSet of() {
    return ImmutableElementSet.EMPTY;
  }

  static ElementSet of(Element element) {
    return ImmutableElementSet.from(1 << element.ordinal());
  }

  static ElementSet copyOf(Iterable<Element> c) {
    if (c instanceof ImmutableElementSet other) {
      return other;
    }
    int value;
    if (c instanceof ElementSet other) {
      value = other.elements();
    } else {
      value = 0;
      for (Element element : c) {
        value |= (1 << element.ordinal());
      }
    }
    return ImmutableElementSet.from(value);
  }
}
