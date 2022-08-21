/*
 * Copyright 2020-2022 Moros
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

import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;

/**
 * Called when a {@link BendingEffect} is applied to a target.
 */
public class TickEffectEvent extends BendingEvent implements UserEvent, Cancellable {
  private final User user;
  private final Entity target;
  private final BendingEffect type;

  private boolean cancelled = false;
  private int duration;

  TickEffectEvent(User user, Entity target, int duration, BendingEffect type) {
    this.user = user;
    this.target = target;
    this.duration = duration;
    this.type = type;
  }

  @Override
  public User user() {
    return user;
  }

  /**
   * Provides the target this bending effect applies to.
   * @return the target entity
   */
  public Entity target() {
    return target;
  }

  /**
   * Provides the duration of the effect in ticks.
   * @return how long the action limit will last
   */
  public int duration() {
    return duration;
  }


  /**
   * Sets the duration of the effect in ticks.
   * @param duration the new duration, must be positive
   */
  public void duration(int duration) {
    if (duration > 0) {
      this.duration = duration;
    }
  }

  /**
   * Provides the type of bending effect that is being applied.
   * @return the effect type
   */
  public BendingEffect type() {
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