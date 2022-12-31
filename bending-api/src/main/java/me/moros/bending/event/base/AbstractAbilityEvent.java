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

package me.moros.bending.event.base;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;

/**
 * Called when a user's ability cooldown has expired.
 */
public abstract class AbstractAbilityEvent extends AbstractUserEvent implements AbilityEvent {
  private final AbilityDescription desc;

  protected AbstractAbilityEvent(User user, AbilityDescription desc) {
    super(user);
    this.desc = desc;
  }

  @Override
  public AbilityDescription ability() {
    return desc;
  }
}
