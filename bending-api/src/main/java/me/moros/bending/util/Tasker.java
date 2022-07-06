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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
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

  public void init(@NonNull Plugin plugin) {
    this.plugin = plugin;
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

  public static @NonNull CompletableFuture<Void> async(@NonNull Runnable runnable) {
    return CompletableFuture.runAsync(runnable, INSTANCE.executor);
  }

  public static <T> @NonNull CompletableFuture<@Nullable T> async(@NonNull Supplier<@Nullable T> supplier) {
    return CompletableFuture.supplyAsync(supplier, INSTANCE.executor);
  }

  public static @Nullable BukkitTask sync(@NonNull Runnable runnable, long delay) {
    if (INSTANCE.plugin != null && INSTANCE.plugin.isEnabled()) {
      return Bukkit.getScheduler().runTaskLater(INSTANCE.plugin, runnable, delay);
    } else {
      runnable.run();
    }
    return null;
  }

  public static @Nullable BukkitTask repeat(@NonNull Runnable runnable, long interval) {
    if (INSTANCE.plugin != null && INSTANCE.plugin.isEnabled()) {
      return Bukkit.getScheduler().runTaskTimer(INSTANCE.plugin, runnable, 1, interval);
    }
    return null;
  }
}
