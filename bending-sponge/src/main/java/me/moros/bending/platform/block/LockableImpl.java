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

package me.moros.bending.platform.block;

import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;

public record LockableImpl(DataHolder.Mutable handle) implements Lockable {
  @Override
  public Optional<String> lock() {
    return handle.get(Keys.LOCK_TOKEN);
  }

  @Override
  public void lock(Component lock) {
    lock(LegacyComponentSerializer.legacySection().serializeOrNull(lock));
  }

  @Override
  public void lock(@Nullable String lock) {
    if (lock == null || lock.isEmpty()) {
      unlock();
    } else {
      handle.offer(Keys.LOCK_TOKEN, lock);
    }
  }

  @Override
  public void unlock() {
    handle.offer(Keys.LOCK_TOKEN, "");
  }
}
