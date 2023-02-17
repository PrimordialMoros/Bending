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

package me.moros.bending.api.event;

import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.BendingEffect;

/**
 * Called when a {@link BendingEffect} is applied to a target.
 */
public interface TickEffectEvent extends UserEvent, Cancellable {
  /**
   * Provides the target this bending effect applies to.
   * @return the target entity
   */
  Entity target();

  /**
   * Provides the duration of the effect in ticks.
   * @return how long the action limit will last
   */
  int duration();

  /**
   * Sets the duration of the effect in ticks.
   * @param duration the new duration, must be positive
   */
  void duration(int duration);

  /**
   * Provides the type of bending effect that is being applied.
   * @return the effect type
   */
  BendingEffect type();
}
