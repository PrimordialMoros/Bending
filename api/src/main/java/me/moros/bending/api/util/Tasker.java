/*
 * Copyright 2020-2024 Moros
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

import java.util.concurrent.Executors;

import me.moros.tasker.executor.AsyncExecutor;
import me.moros.tasker.executor.SimpleAsyncExecutor;
import me.moros.tasker.executor.SyncExecutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to easily schedule sync, async tasks.
 */
public final class Tasker {
  private Tasker() {
  }

  private static SyncExecutor SYNC;
  private static final AsyncExecutor ASYNC;

  static {
    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    ASYNC = new SimpleAsyncExecutor(Executors.newScheduledThreadPool(threads));
  }

  /**
   * Get the sync task executor.
   * @return the sync task executor
   */
  public static @MonotonicNonNull SyncExecutor sync() {
    return SYNC;
  }

  /**
   * Get the async task executor.
   * @return the async task executor
   */
  public static @NonNull AsyncExecutor async() {
    return ASYNC;
  }
}
