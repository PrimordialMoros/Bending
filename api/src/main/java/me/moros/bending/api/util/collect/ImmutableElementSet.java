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

package me.moros.bending.api.util.collect;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.collect.MutableElementSet.Itr;

final class ImmutableElementSet extends AbstractSet<Element> implements ElementSet {
  private static final int SPLITERATOR_CHARACTERISTICS = MutableElementSet.SPLITERATOR_CHARACTERISTICS | Spliterator.IMMUTABLE;

  static final ElementSet EMPTY = new ImmutableElementSet(0);

  private final int elements;

  private ImmutableElementSet(int elements) {
    this.elements = elements;
  }

  static ElementSet from(int value) {
    return value == 0 ? EMPTY : new ImmutableElementSet(value);
  }

  @Override
  public int elements() {
    return elements;
  }

  @Override
  public int size() {
    return Integer.bitCount(elements);
  }

  @Override
  public boolean isEmpty() {
    return elements == 0;
  }

  @Override
  public boolean contains(Object e) {
    if (e == null || e.getClass() != Element.class) {
      return false;
    }
    return (elements & (1 << ((Element) e).ordinal())) != 0;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return c instanceof ElementSet other ? ((other.elements() & ~elements) == 0) : super.containsAll(c);
  }

  @Override
  public Iterator<Element> iterator() {
    return new Itr(elements);
  }

  @Override
  public Spliterator<Element> spliterator() {
    return Spliterators.spliterator(this, SPLITERATOR_CHARACTERISTICS);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ElementSet other ? (other.elements() == elements) : super.equals(obj);
  }

  @Override
  public boolean set(Set<Element> other) {
    throw uoe();
  }

  @Override
  public boolean add(Element e) {
    throw uoe();
  }

  @Override
  public boolean addAll(Collection<? extends Element> c) {
    throw uoe();
  }

  @Override
  public void clear() {
    throw uoe();
  }

  @Override
  public boolean remove(Object o) {
    throw uoe();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw uoe();
  }

  @Override
  public boolean removeIf(Predicate<? super Element> filter) {
    throw uoe();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw uoe();
  }

  private static UnsupportedOperationException uoe() {
    return new UnsupportedOperationException();
  }
}
