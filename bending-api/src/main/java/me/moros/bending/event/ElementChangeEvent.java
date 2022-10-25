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
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Called when a user's elements are being changed.
 */
public class ElementChangeEvent extends Event implements UserEvent, Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final User user;
  private final ElementAction action;

  private boolean cancelled = false;

  ElementChangeEvent(User user, ElementAction action) {
    this.user = user;
    this.action = action;
  }

  @Override
  public User user() {
    return user;
  }

  /**
   * Provides the type of change for this event.
   * @return the type of element change
   */
  public ElementAction type() {
    return action;
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
   * Represents a type of element change.
   */
  public enum ElementAction {
    /**
     * One element is selected and all others are removed.
     */
    CHOOSE,
    /**
     * An element is added.
     */
    ADD,
    /**
     * An element is removed.
     */
    REMOVE
  }
}
