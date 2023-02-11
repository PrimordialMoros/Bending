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

package me.moros.bending.sponge.platform.block;

import java.util.Optional;

import me.moros.bending.api.platform.block.Lockable;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;

public record LockableImpl(DataHolder.Mutable handle) implements Lockable {
  @Override
  public Optional<String> lock() {
    return handle.get(Keys.LOCK_TOKEN).filter(s -> !s.isBlank());
  }

  @Override
  public void lock(String lock) {
    if (lock.isBlank()) {
      handle.offer(Keys.LOCK_TOKEN, "");
    } else {
      handle.offer(Keys.LOCK_TOKEN, lock);
    }
  }
}
