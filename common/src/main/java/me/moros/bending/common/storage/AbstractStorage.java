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

package me.moros.bending.common.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;

import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

abstract class AbstractStorage implements BendingStorage {
  protected final Logger logger;
  private final Executor executor;

  protected AbstractStorage(Logger logger) {
    this.logger = logger;
    this.executor = Tasker.async(); // TODO replace with loom in JDK 21
  }

  private <R> CompletableFuture<R> async(Supplier<R> supplier) {
    return CompletableFuture.supplyAsync(supplier, executor);
  }

  private CompletableFuture<Void> async(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, executor);
  }

  @Override
  public final CompletableFuture<Set<UUID>> loadUuidsAsync() {
    return async(this::loadUuids).exceptionally(logError(Set.of()));
  }

  @Override
  public final CompletableFuture<@Nullable BenderProfile> loadProfileAsync(UUID uuid) {
    return async(() -> loadProfile(uuid)).exceptionally(logError(null));
  }

  @Override
  public final Map<UUID, BenderProfile> loadProfiles(Set<UUID> uuids) {
    Map<UUID, BenderProfile> results = new HashMap<>(uuids.size());
    for (UUID uuid : uuids) {
      results.computeIfAbsent(uuid, this::loadProfile);
    }
    return results;
  }

  @Override
  public final CompletableFuture<Map<UUID, BenderProfile>> loadProfilesAsync(Set<UUID> uuids, LongAdder progressCounter) {
    final int size = uuids.size();
    Map<UUID, BenderProfile> results = new ConcurrentHashMap<>(size);
    CompletableFuture<?>[] futures = new CompletableFuture[size];
    AtomicInteger counter = new AtomicInteger();
    for (UUID uuid : uuids) {
      futures[counter.getAndIncrement()] = async(() -> {
        results.computeIfAbsent(uuid, this::loadProfile);
        progressCounter.increment();
      });
    }
    return CompletableFuture.allOf(futures).handle((ignore, t) -> {
      if (t != null) {
        logger.warn(t.getMessage(), t);
      }
      return results;
    });
  }

  @Override
  public final CompletableFuture<Boolean> saveProfileAsync(BenderProfile profile) {
    return async(() -> saveProfile(profile)).exceptionally(logError(false));
  }

  @Override
  public final boolean saveProfiles(Collection<BenderProfile> profiles) {
    boolean result = false;
    for (var profile : profiles) {
      result |= saveProfile(profile);
    }
    return result;
  }

  @Override
  public final CompletableFuture<Boolean> saveProfilesAsync(Collection<BenderProfile> profiles, LongAdder progressCounter) {
    final int size = profiles.size();
    CompletableFuture<?>[] futures = new CompletableFuture[size];
    AtomicInteger counter = new AtomicInteger();
    LongAdder successful = new LongAdder();
    for (var profile : profiles) {
      futures[counter.getAndIncrement()] = async(() -> {
        if (saveProfile(profile)) {
          successful.increment();
        }
        progressCounter.increment();
      });
    }
    if (futures.length == 0) {
      return CompletableFuture.completedFuture(false);
    }
    return CompletableFuture.allOf(futures).handle((ignore, t) -> {
      if (t != null) {
        logger.warn(t.getMessage(), t);
      }
      return successful.intValue() == size;
    });
  }

  private <R> Function<Throwable, @PolyNull R> logError(@PolyNull R def) {
    return t -> {
      logger.error(t.getMessage(), t);
      return def;
    };
  }
}
