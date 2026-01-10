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

package me.moros.bending.api.game;

import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.user.User;

/**
 * Handles the registration of sequence steps and activates matching sequences.
 */
public interface SequenceManager {
  /**
   * Register a sequence step.
   * @param user the user to associate the step with
   * @param action the activation of the user
   */
  void registerStep(User user, Activation action);
}
