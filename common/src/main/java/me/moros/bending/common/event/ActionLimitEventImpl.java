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

package me.moros.bending.common.event;

import me.moros.bending.api.event.ActionLimitEvent;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.common.event.base.AbstractCancellableUserEvent;

public class ActionLimitEventImpl extends AbstractCancellableUserEvent implements ActionLimitEvent {
  private final LivingEntity target;

  private long duration;

  public ActionLimitEventImpl(User user, LivingEntity target, long duration) {
    super(user);
    this.target = target;
    this.duration = duration;
  }

  @Override
  public LivingEntity target() {
    return target;
  }

  @Override
  public long duration() {
    return duration;
  }

  @Override
  public void duration(long duration) {
    this.duration = duration;
  }
}
