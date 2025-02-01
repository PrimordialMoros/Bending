/*
 * Copyright 2020-2025 Moros
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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an event that is called when a user's binds change.
 */
public interface BindChangeEvent extends UserEvent, Cancellable {
  /**
   * Called when a user attempts to bind or clear an ability slot.
   */
  interface Single extends BindChangeEvent {
    /**
     * Provides the slot that is changed.
     * @return the slot index in the range [1, 9] (inclusive).
     */
    int slot();

    /**
     * Provides the ability that is changed.
     * @return the ability that is bound or null if the slot is cleared
     */
    @Nullable AbilityDescription ability();
  }

  /**
   * Called when multiple binds of a user change.
   */
  interface Multi extends BindChangeEvent {
    /**
     * Provides the preset that is being bound.
     * @return the preset of abilities
     */
    Preset preset();
  }
}
