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

package me.moros.bending.common.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class Debouncer<R> {
  private final Supplier<R> supplier;
  private final Executor executor;
  private final CompletableFuture<R> future;
  private final AtomicReference<CompletableFuture<R>> taskRef;

  private Debouncer(Supplier<R> supplier, Executor executor) {
    this.supplier = supplier;
    this.executor = executor;
    this.future = new CompletableFuture<>();
    this.taskRef = new AtomicReference<>();
  }

  public CompletableFuture<R> future() {
    return future;
  }

  public CompletableFuture<R> request() {
    taskRef.updateAndGet(this::createOrReschedule);
    return future;
  }

  private CompletableFuture<R> createOrReschedule(@Nullable CompletableFuture<R> taskFuture) {
    if (taskFuture != null) {
      taskFuture.cancel(false);
    }
    CompletableFuture<R> newFuture = CompletableFuture.supplyAsync(supplier, executor);
    newFuture.thenAccept(future::complete);
    return newFuture;
  }

  public static <R> Debouncer<R> create(Supplier<R> supplier, long delay, TimeUnit timeUnit) {
    Objects.requireNonNull(supplier);
    return new Debouncer<>(supplier, CompletableFuture.delayedExecutor(delay, timeUnit));
  }

  public static Debouncer<?> create(Runnable runnable, long delay, TimeUnit timeUnit) {
    Objects.requireNonNull(runnable);
    return create(() -> {
      runnable.run();
      return null;
    }, delay, timeUnit);
  }
}
