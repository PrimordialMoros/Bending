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

package me.moros.bending.model.manager;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.user.User;

/**
 * FlightManager keeps track of flight references.
 * Every time a reference is acquired, it should eventually be released.
 * If no references remain the user will lose flight.
 */
public interface FlightManager extends Updatable {
  boolean hasFlight(User user);

  Flight get(User user);

  void remove(User user);

  void removeAll();

  /**
   * Represents a flight reference.
   */
  interface Flight {
    User user();

    void flying(boolean value);

    /**
     * Release a flight reference
     */
    void release();
  }
}
