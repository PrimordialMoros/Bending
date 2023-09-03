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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import me.moros.bending.api.util.Tasker;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Debounced<R> {
  private final Supplier<R> supplier;
  private final long delay;
  private final TimeUnit timeUnit;
  private final CompletableFuture<R> future;
  private final AtomicReference<CompletableFuture<R>> taskRef;

  private Debounced(Supplier<R> supplier, long delay, TimeUnit timeUnit) {
    this.supplier = supplier;
    this.delay = delay;
    this.timeUnit = timeUnit;
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
    CompletableFuture<R> newFuture = Tasker.async().submit(supplier, delay, timeUnit);
    newFuture.thenAccept(future::complete);
    return newFuture;
  }

  public static <R> Debounced<R> create(Supplier<R> supplier, long delay, TimeUnit timeUnit) {
    Objects.requireNonNull(supplier);
    return new Debounced<>(supplier, delay, timeUnit);
  }

  public static Debounced<?> create(Runnable runnable, long delay, TimeUnit timeUnit) {
    Objects.requireNonNull(runnable);
    return create(() -> {
      runnable.run();
      return null;
    }, delay, timeUnit);
  }
}
