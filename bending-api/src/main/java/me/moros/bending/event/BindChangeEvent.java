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

import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.User;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called when a user attempts to bind an ability or {@link Preset}.
 */
public class BindChangeEvent extends Event implements UserEvent, Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final User user;
  private final BindType type;

  private boolean cancelled = false;

  BindChangeEvent(User user, BindType type) {
    this.user = user;
    this.type = type;
  }

  @Override
  public User user() {
    return user;
  }

  /**
   * Provides the type of binding
   * @return the type
   */
  public BindType type() {
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

  @Override
  public @NonNull HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  /**
   * Represents a type of bind change.
   */
  public enum BindType {
    /**
     * Represents binding of a single ability.
     */
    SINGLE,
    /**
     * Represents binding of a preset.
     */
    MULTIPLE
  }
}
