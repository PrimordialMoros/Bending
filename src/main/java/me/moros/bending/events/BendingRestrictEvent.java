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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.user.User;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

public class BendingRestrictEvent extends BendingUserEvent implements Cancellable {
  private final LivingEntity target;

  private boolean cancelled = false;
  private long duration;

  BendingRestrictEvent(User user, LivingEntity target, long duration) {
    super(user);
    this.target = target;
    this.duration = duration;
  }

  public @NonNull LivingEntity target() {
    return target;
  }

  public long duration() {
    return duration;
  }

  public void duration(long duration) {
    this.duration = duration;
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
