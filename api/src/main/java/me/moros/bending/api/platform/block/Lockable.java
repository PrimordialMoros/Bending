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

package me.moros.bending.api.platform.block;

import me.moros.bending.api.platform.item.ItemSnapshot;

public interface Lockable {
  boolean hasLock();

  @Deprecated(forRemoval = true, since = "3.12.0")
  default boolean canUnlock(ItemSnapshot item) {
    return false;
  }

  @Deprecated(forRemoval = true, since = "3.12.0")
  default void lock(ItemSnapshot item) {
  }

  void unlock();
}
