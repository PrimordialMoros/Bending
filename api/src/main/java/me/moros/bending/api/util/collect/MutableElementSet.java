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

package me.moros.bending.api.util.collect;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;

import me.moros.bending.api.ability.element.Element;
import org.jspecify.annotations.Nullable;

final class MutableElementSet extends AbstractSet<Element> implements ElementSet {
  static final int SPLITERATOR_CHARACTERISTICS = Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.SIZED |
    Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT;

  private static final Element[] UNIVERSE = Element.values().clone();

  private final AtomicInteger elements;

  MutableElementSet() {
    elements = new AtomicInteger(0);
  }

  @Override
  public int elements() {
    return elements.get();
  }

  @Override
  public int size() {
    return Integer.bitCount(elements.get());
  }

  @Override
  public boolean isEmpty() {
    return elements.get() == 0;
  }

  @Override
  public boolean contains(Object e) {
    if (e == null || e.getClass() != Element.class) {
      return false;
    }
    return (elements.get() & (1 << ((Element) e).ordinal())) != 0;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return c instanceof ElementSet other ? ((other.elements() & ~elements.get()) == 0) : super.containsAll(c);
  }

  @Override
  public boolean set(@Nullable Set<Element> other) {
    if (other == null) {
      return false;
    }
    int value;
    if (other instanceof ElementSet otherElementSet) {
      value = otherElementSet.elements();
    } else {
      value = 0;
      for (Element element : other) {
        value |= (1 << element.ordinal());
      }
    }
    return elements.getAndSet(value) != value;
  }

  @Override
  public boolean add(@Nullable Element e) {
    if (e == null) {
      return false;
    }
    final int modifier = 1 << e.ordinal();
    return elements.getAndUpdate(curr -> curr | modifier) != elements.get();
  }

  @Override
  public boolean remove(@Nullable Object e) {
    if (e == null || e.getClass() != Element.class) {
      return false;
    }
    final int modifier = ~(1 << ((Element) e).ordinal());
    return elements.getAndUpdate(curr -> curr & modifier) != elements.get();
  }

  @Override
  public boolean addAll(Collection<? extends Element> c) {
    if (!(c instanceof ElementSet other)) {
      return super.addAll(c);
    }
    final int modifier = other.elements();
    return elements.getAndUpdate(curr -> curr | modifier) != elements.get();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (!(c instanceof ElementSet other)) {
      return super.removeAll(c);
    }
    final int modifier = ~other.elements();
    return elements.getAndUpdate(curr -> curr & modifier) != elements.get();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    if (!(c instanceof ElementSet other)) {
      return super.retainAll(c);
    }
    final int modifier = other.elements();
    return elements.getAndUpdate(curr -> curr & modifier) != elements.get();
  }

  @Override
  public void clear() {
    elements.set(0);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ElementSet other ? (other.elements() == elements.get()) : super.equals(obj);
  }

  @Override
  public Iterator<Element> iterator() {
    return new Itr(elements.get());
  }

  @Override
  public Spliterator<Element> spliterator() {
    return Spliterators.spliterator(this, SPLITERATOR_CHARACTERISTICS);
  }

  // Iterator on snapshot
  static final class Itr implements Iterator<Element> {
    private int unseen;

    Itr(int elements) {
      unseen = elements;
    }

    @Override
    public boolean hasNext() {
      return unseen != 0;
    }

    @Override
    public Element next() {
      if (unseen == 0) {
        throw new NoSuchElementException();
      }
      int lastReturned = unseen & -unseen;
      unseen -= lastReturned;
      return UNIVERSE[Integer.numberOfTrailingZeros(lastReturned)];
    }
  }
}
