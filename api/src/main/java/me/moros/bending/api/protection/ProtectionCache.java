/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.protection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;

/**
 * A multi-layered cache used to check if a User can build in a specific block location.
 * While this implementation is thread-safe it might be dangerous to use this async as the protection plugins
 * might not be thread-safe themselves and data is fetched from those when results aren't cached.
 */
public enum ProtectionCache {
  INSTANCE;

  private final Map<UUID, LoadingCache<Block, Boolean>> cache;

  ProtectionCache() {
    cache = new ConcurrentHashMap<>();
  }

  /**
   * Remove the block protection cache for the specified user id if it exists.
   * @param uuid the user's id
   */
  public void invalidate(UUID uuid) {
    cache.remove(uuid);
  }

  /**
   * Checks if a user can build at a block location. First it queries the cache.
   * If no result is found it computes it and adds it to the cache before returning the result.
   * @param user the user to check
   * @param block the block to check
   * @return the result
   * @see #canBuildPostCache(User, Block)
   */
  public boolean canBuild(User user, Block block) {
    return cache.computeIfAbsent(user.uuid(), u -> buildCache(user)).get(block);
  }

  /**
   * Checks if a user can build at a block location.
   * @param user the user to check
   * @param block the block to check
   * @return true if all enabled protections allow it, false otherwise
   */
  private boolean canBuildPostCache(User user, Block block) {
    return Registries.PROTECTIONS.stream().allMatch(m -> m.canBuild(user, block));
  }

  /**
   * Creates a block cache in which entries expire 5000ms after their last access time.
   * @return the created cache
   * @see Caffeine
   */
  private LoadingCache<Block, Boolean> buildCache(User user) {
    return Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build(b -> canBuildPostCache(user, b));
  }
}
