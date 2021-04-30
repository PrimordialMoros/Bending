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

import java.time.Duration;
import java.util.Set;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;

public class Expiring<T> {
  private final Cache<T, Boolean> cache;

  public Expiring(long duration) {
    cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(duration)).build();
  }

  public void add(@NonNull T key) {
    cache.put(key, false);
  }

  public boolean contains(@NonNull T key) {
    return cache.getIfPresent(key) != null;
  }

  public boolean isEmpty() {
    cache.cleanUp();
    return cache.asMap().isEmpty();
  }

  public @NonNull Set<T> all() {
    cache.cleanUp();
    return Set.copyOf(cache.asMap().keySet());
  }
}
