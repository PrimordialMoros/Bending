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

package me.moros.bending.api.event;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.event.base.AbstractCancellableUserEvent;
import me.moros.bending.api.event.base.UserEvent;
import me.moros.bending.api.user.User;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an event that is called when a user's binds change.
 */
public interface BindChangeEvent extends UserEvent {
  /**
   * Provides the type of binding
   * @return the type
   */
  BindType type();

  /**
   * Represents a type of bind change.
   */
  enum BindType {
    /**
     * Represents binding of a single ability.
     */
    SINGLE,
    /**
     * Represents binding of a preset.
     */
    MULTIPLE
  }

  /**
   * Called when a user attempts to bind or clear an ability slot.
   */
  class Single extends AbstractCancellableUserEvent implements BindChangeEvent {
    private final int slot;
    private final AbilityDescription desc;

    protected Single(User user, int slot, @Nullable AbilityDescription desc) {
      super(user);
      this.slot = slot;
      this.desc = desc;
    }

    /**
     * Provides the slot that is changed.
     * @return the slot index in the range [1, 9] (inclusive).
     */
    public int slot() {
      return slot;
    }

    /**
     * Provides the ability that is changed.
     * @return the ability that is bound or null if the slot is cleared
     */
    public @Nullable AbilityDescription ability() {
      return desc;
    }

    @Override
    public BindType type() {
      return BindType.SINGLE;
    }
  }

  /**
   * Called when multiple binds of a user change.
   */
  class Multi extends AbstractCancellableUserEvent implements BindChangeEvent {
    private final Preset preset;

    protected Multi(User user, Preset preset) {
      super(user);
      this.preset = preset;
    }

    /**
     * Provides the preset that is being bound.
     * @return the preset of abilities
     */
    public Preset preset() {
      return preset;
    }

    @Override
    public BindType type() {
      return BindType.MULTIPLE;
    }
  }
}
