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

package me.moros.bending.common.event;

import me.moros.bending.api.event.TickEffectEvent;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.common.event.base.AbstractCancellableUserEvent;

public class TickEffectEventImpl extends AbstractCancellableUserEvent implements TickEffectEvent {
  private final Entity target;
  private final BendingEffect type;

  private int duration;

  public TickEffectEventImpl(User user, Entity target, int duration, BendingEffect type) {
    super(user);
    this.target = target;
    this.duration = duration;
    this.type = type;
  }

  @Override
  public Entity target() {
    return target;
  }

  @Override
  public int duration() {
    return duration;
  }

  @Override
  public void duration(int duration) {
    if (duration > 0) {
      this.duration = duration;
    }
  }

  @Override
  public BendingEffect type() {
    return type;
  }
}
