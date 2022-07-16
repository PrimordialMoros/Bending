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

package me.moros.bending.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to easily schedule sync, async tasks.
 */
public enum Tasker {
  INSTANCE;

  private final ExecutorService executor;
  private Plugin plugin;

  Tasker() {
    executor = Executors.newCachedThreadPool();
  }

  public void inject(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin);
  }

  public void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }

  private boolean canExecute() {
    return plugin != null && plugin.isEnabled();
  }

  /**
   * Create a synchronous task and execute it after a delay.
   * @param runnable the task to execute
   * @param delay the delay in ticks
   * @return the created task if scheduled, false otherwise
   */
  public @Nullable BukkitTask sync(Runnable runnable, long delay) {
    return canExecute() ? plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delay) : null;
  }

  /**
   * Create a repeating synchronous tasks and execute it.
   * @param runnable the task to execute
   * @param interval the interval in ticks
   * @return the created task if scheduled, false otherwise
   */
  public @Nullable BukkitTask repeat(Runnable runnable, long interval) {
    return canExecute() ? plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, 1, interval) : null;
  }

  /**
   * Create an asynchronous task using Bending's executor.
   * @param runnable the task to execute
   * @return a future
   */
  public static CompletableFuture<Void> async(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, INSTANCE.executor);
  }

  /**
   * Create an asynchronous task using Bending's executor.
   * @param supplier the task to execute
   * @return a future with the result
   */
  public static <T> CompletableFuture<@Nullable T> async(Supplier<@Nullable T> supplier) {
    return CompletableFuture.supplyAsync(supplier, INSTANCE.executor);
  }
}
