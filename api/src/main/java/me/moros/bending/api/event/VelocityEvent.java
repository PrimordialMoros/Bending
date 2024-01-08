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
import me.moros.math.Vector3d;

/**
 * Called when an ability attempts to alter the velocity of a LivingEntity.
 */
public interface VelocityEvent extends AbilityEvent, Cancellable {
  /**
   * Provides the target whose velocity is being changed.
   * @return the LivingEntity target
   */
  LivingEntity target();

  /**
   * Provides the new velocity that will be applied to the target.
   * @return the new velocity
   */
  Vector3d velocity();

  /**
   * Sets the new velocity that will be applied to the target.
   * @param velocity the velocity to apply
   */
  void velocity(Vector3d velocity);
}
