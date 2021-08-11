/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.events;

import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BendingTickEffectEvent extends BendingUserEvent implements Cancellable {
  private final Entity target;
  private final BendingEffect type;

  private boolean cancelled = false;
  private int duration;

  BendingTickEffectEvent(User user, Entity target, int duration, BendingEffect type) {
    super(user);
    this.target = target;
    this.duration = duration;
    this.type = type;
  }

  public @NonNull Entity target() {
    return target;
  }

  public int duration() {
    return duration;
  }

  public void duration(int duration) {
    if (duration > 0) {
      this.duration = duration;
    }
  }

  public @NonNull BendingEffect type() {
    return type;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
