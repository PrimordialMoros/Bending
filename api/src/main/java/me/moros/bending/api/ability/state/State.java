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

package me.moros.bending.api.ability.state;

import me.moros.bending.api.ability.Updatable;

/**
 * Represents an {@link Updatable} state that can be started and completed.
 * @see StateChain
 */
public interface State extends Updatable {
  /**
   * Start this state.
   * @param chain the chain to assign this state to
   */
  void start(StateChain chain);

  /**
   * Complete this state.
   */
  void complete();
}
