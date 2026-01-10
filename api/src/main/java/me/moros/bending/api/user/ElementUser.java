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

package me.moros.bending.api.user;

import java.util.Collection;
import java.util.Set;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.event.ElementChangeEvent;

/**
 * Represents a user that has a set of {@link Element}.
 */
public interface ElementUser {
  /**
   * Get a copy of this user's elements.
   * @return a copy of this user's elements
   */
  Set<Element> elements();

  /**
   * Check if the user has the specified element.
   * @param element the element to check
   * @return true the user has the specified element, false otherwise
   */
  boolean hasElement(Element element);

  /**
   * Check if the user has all the specified elements.
   * @param elements the elements to check
   * @return true the user has the specified elements, false otherwise
   */
  boolean hasElements(Collection<Element> elements);

  /**
   * Attempt to add the specified element to the user. Calls an {@link ElementChangeEvent}.
   * @param element the element to add
   * @return true if the element was added successfully, false otherwise
   */
  boolean addElement(Element element);

  /**
   * Attempt to remove the specified element from the user. Calls an {@link ElementChangeEvent}.
   * @param element the element to remove
   * @return true if the element was removed successfully, false otherwise
   */
  boolean removeElement(Element element);

  /**
   * Attempt to choose the specified element. Calls an {@link ElementChangeEvent}.
   * @param element the element to choose
   * @return true if the element was chosen successfully, false otherwise
   */
  boolean chooseElement(Element element);
}
