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

package me.moros.bending.event;

import me.moros.bending.event.base.AbstractCancellableUserEvent;
import me.moros.bending.event.base.UserEvent;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.temporal.ActionLimiter;

/**
 * Called when a user attempts to limit the actions of a target through bending.
 * @see ActionLimiter
 */
public class ActionLimitEvent extends AbstractCancellableUserEvent implements UserEvent {
  private final LivingEntity target;

  private long duration;

  protected ActionLimitEvent(User user, LivingEntity target, long duration) {
    super(user);
    this.target = target;
    this.duration = duration;
  }

  /**
   * Provides the entity that is affected by the {@link ActionLimiter}
   * @return the target entity
   */
  public LivingEntity target() {
    return target;
  }

  /**
   * Provides the duration of the restriction in milliseconds.
   * @return how long the action limit will last
   */
  public long duration() {
    return duration;
  }

  /**
   * Sets the duration of the restriction in milliseconds.
   * @param duration the new duration
   */
  public void duration(long duration) {
    this.duration = duration;
  }
}
