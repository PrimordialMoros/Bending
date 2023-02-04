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

package me.moros.bending.api.ability.element;

import me.moros.bending.api.user.User;
import net.kyori.adventure.audience.Audience;

/**
 * Represents a handler for Element actions.
 */
public interface ElementHandler {
  /**
   * Perform an action when an element is chosen for a user.
   * @param user the user
   * @param element the element being chosen
   */
  void onElementChoose(User user, Element element);

  /**
   * Perform an action when an element is added to a user.
   * @param user the user
   * @param element the element being added
   */
  void onElementAdd(User user, Element element);

  /**
   * Perform an action when an element is removed from a user.
   * @param user the user
   * @param element the element being removed
   */
  void onElementRemove(User user, Element element);

  /**
   * Perform an action when an element is displayed to a user.
   * @param user the user
   * @param element the element being displayed
   */
  void onElementDisplay(Audience user, Element element);
}
