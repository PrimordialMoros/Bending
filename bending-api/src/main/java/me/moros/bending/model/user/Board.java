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

package me.moros.bending.model.user;

import me.moros.bending.model.ability.AbilityDescription;

public interface Board {
  String SEP = " -------------- ";

  default boolean isEnabled() {
    return false;
  }

  default void disableScoreboard() {
  }

  default void updateSlot(int slot) {
  }

  default void updateAll() {
  }

  default void activeSlot(int oldSlot, int newSlot) {
  }

  default void updateMisc(AbilityDescription desc, boolean show) {
  }
}
