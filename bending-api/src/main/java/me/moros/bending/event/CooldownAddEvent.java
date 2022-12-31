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

import me.moros.bending.event.base.AbilityEvent;
import me.moros.bending.event.base.AbstractCancellableAbilityEvent;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;

/**
 * Called when a user's ability will go on cooldown.
 */
public class CooldownAddEvent extends AbstractCancellableAbilityEvent implements AbilityEvent {
  private final long duration;

  protected CooldownAddEvent(User user, AbilityDescription desc, long duration) {
    super(user, desc);
    this.duration = duration;
  }

  /**
   * Provides the cooldown duration.
   * @return the cooldown's duration in milliseconds
   */
  public long duration() {
    return duration;
  }
}
