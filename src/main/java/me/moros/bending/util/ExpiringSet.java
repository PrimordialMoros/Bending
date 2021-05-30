/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.atlas.caffeine.cache.Scheduler;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ExpiringSet<E> {
  private final Cache<E, Boolean> cache;

  public ExpiringSet(long duration) {
    this(duration, TimeUnit.MILLISECONDS);
  }

  public ExpiringSet(long duration, @NonNull TimeUnit unit) {
    cache = Caffeine.newBuilder()
      .expireAfterWrite(duration, unit)
      .scheduler(Scheduler.systemScheduler())
      .build();
  }

  public void add(@NonNull E item) {
    cache.put(item, false);
  }

  public boolean contains(@NonNull E item) {
    return cache.getIfPresent(item) != null;
  }

  public @NonNull Set<@NonNull E> snapshot() {
    return Set.copyOf(cache.asMap().keySet());
  }
}
