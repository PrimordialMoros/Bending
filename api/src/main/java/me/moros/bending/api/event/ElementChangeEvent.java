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

package me.moros.bending.api.event;

import me.moros.bending.api.ability.element.Element;

/**
 * Called when a user's elements are being changed.
 */
public interface ElementChangeEvent extends UserEvent, Cancellable {
  /**
   * Provide the element associated with the change.
   * @return the element
   */
  Element element();

  /**
   * Provides the type of change for this event.
   * @return the type of element change
   */
  ElementAction type();

  /**
   * Represents a type of element change.
   */
  enum ElementAction {
    /**
     * One element is selected and all others are removed.
     */
    CHOOSE,
    /**
     * An element is added.
     */
    ADD,
    /**
     * An element is removed.
     */
    REMOVE
  }
}
