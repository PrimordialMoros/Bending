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

import me.moros.tasker.executor.AsyncExecutor;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.executor.SyncExecutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Utility class to easily schedule sync, async tasks.
 */
public final class Tasker {
  private Tasker() {
  }

  private static CompositeExecutor EXECUTOR;

  /**
   * Get the sync task executor.
   * @return the sync task executor
   */
  public static @MonotonicNonNull SyncExecutor sync() {
    return EXECUTOR.sync();
  }

  /**
   * Get the async task executor.
   * @return the async task executor
   */
  public static @MonotonicNonNull AsyncExecutor async() {
    return EXECUTOR.async();
  }
}
