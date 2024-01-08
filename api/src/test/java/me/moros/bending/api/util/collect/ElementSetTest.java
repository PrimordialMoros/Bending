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

import java.util.HashSet;
import java.util.Set;

import me.moros.bending.api.ability.element.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementSetTest {
  @Test
  void testEmptyElementSetEquality() {
    Set<Element> hashSet = new HashSet<>();
    Set<Element> mutableElementSet = ElementSet.mutable();
    Set<Element> immutableElementSet = ElementSet.of();

    assertEquals(hashSet, mutableElementSet);
    assertEquals(mutableElementSet, immutableElementSet);
    assertEquals(immutableElementSet, hashSet);
  }

  @Test
  void testElementSetEquality() {
    Element element = Element.FIRE;

    Set<Element> hashSet = new HashSet<>();
    hashSet.add(element);
    Set<Element> immutableSet = Set.of(element);
    Set<Element> mutableElementSet = ElementSet.mutable();
    mutableElementSet.add(element);
    Set<Element> immutableElementSet = ElementSet.of(element);

    assertEquals(hashSet, immutableSet);
    assertEquals(immutableSet, mutableElementSet);
    assertEquals(mutableElementSet, immutableElementSet);
    assertEquals(immutableElementSet, hashSet);
  }
}
