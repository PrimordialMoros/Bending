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

package me.moros.bending.paper.platform;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import me.moros.bending.api.ability.DamageSource;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to handle bending damage and death messages.
 */
public final class DamageUtil {
  private DamageUtil() {
  }

  private static final Cache<UUID, DamageSource> CACHE = Caffeine.newBuilder()
    .expireAfterWrite(100, TimeUnit.MILLISECONDS)
    .scheduler(Scheduler.systemScheduler())
    .build();

  public static void cacheDamageSource(UUID uuid, @Nullable DamageSource source) {
    if (source != null) {
      CACHE.put(uuid, source);
    }
  }

  public static @Nullable DamageSource cachedDamageSource(UUID uuid) {
    return CACHE.getIfPresent(uuid);
  }
}
