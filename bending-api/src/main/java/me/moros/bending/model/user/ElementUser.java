/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.model.user;

import java.util.Set;

import me.moros.bending.event.ElementChangeEvent;
import me.moros.bending.model.Element;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface ElementUser {
  /**
   * @return a copy of this user's elements
   */
  @NonNull Set<@NonNull Element> elements();

  /**
   * Check if the user has the specified element.
   * @param element the element to check
   * @return true the user has the specified element, false otherwise
   */
  boolean hasElement(@NonNull Element element);

  /**
   * Attempt to add the specified element to the user. Calls an {@link ElementChangeEvent}.
   * @param element the element to add
   * @return true if the element was added successfully, false otherwise
   */
  boolean addElement(@NonNull Element element);

  /**
   * Attempt to remove the specified element from the user. Calls an {@link ElementChangeEvent}.
   * @param element the element to remove
   * @return true if the element was removed successfully, false otherwise
   */
  boolean removeElement(@NonNull Element element);

  /**
   * Attempt to choose the specified element. Calls an {@link ElementChangeEvent}.
   * @param element the element to choose
   * @return true if the element was chosen successfully, false otherwise
   */
  boolean chooseElement(@NonNull Element element);
}
