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

package me.moros.bending.api.util;

import java.util.Objects;

import me.moros.tasker.executor.AsyncExecutor;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.executor.SyncExecutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Utility class to easily schedule sync, async tasks.
 */
public final class Tasker {
  private static SyncExecutor SYNC;
  private static AsyncExecutor ASYNC;

  public static void inject(CompositeExecutor compositeExecutor) {
    Objects.requireNonNull(compositeExecutor);
    if (SYNC != null || ASYNC != null) {
      throw new RuntimeException("Executor has already been injected!");
    }
    SYNC = compositeExecutor.sync();
    ASYNC = compositeExecutor.async();
  }

  public static @MonotonicNonNull SyncExecutor sync() {
    return SYNC;
  }

  public static @MonotonicNonNull AsyncExecutor async() {
    return ASYNC;
  }
}
