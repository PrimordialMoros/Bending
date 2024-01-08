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

package me.moros.bending.api.event;

import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.temporal.ActionLimiter;

/**
 * Called when a user attempts to limit the actions of a target through bending.
 * @see ActionLimiter
 */
public interface ActionLimitEvent extends UserEvent, Cancellable {
  /**
   * Provides the entity that is affected by the {@link ActionLimiter}
   * @return the target entity
   */
  LivingEntity target();

  /**
   * Provides the duration of the restriction in milliseconds.
   * @return how long the action limit will last
   */
  long duration();

  /**
   * Sets the duration of the restriction in milliseconds.
   * @param duration the new duration
   */
  void duration(long duration);
}
