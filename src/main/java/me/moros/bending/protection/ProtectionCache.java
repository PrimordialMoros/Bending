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

package me.moros.bending.protection;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A multi-layered cache used to check if a User can build in a specific block location.
 * While this implementation is thread-safe it might be dangerous to use this async as the protection plugins
 * might not be thread-safe themselves and data is fetched from those when results aren't cached.
 */
public enum ProtectionCache {
  INSTANCE;

  private final Map<UUID, Cache<Block, Boolean>> cache;

  ProtectionCache() {
    cache = new ConcurrentHashMap<>();
  }

  /**
   * Remove the block protection cache for the specified user.
   * @param user the user for which the cache will be invalidated
   */
  public void invalidate(@NonNull User user) {
    cache.remove(user.uuid());
  }

  /**
   * Checks if a user can build at a block location. First it queries the cache.
   * If no result is found it computes it and adds it to the cache before returning the result.
   * Harmless actions are automatically allowed if allowHarmless is configured
   * @param user the user to check
   * @param block the block to check
   * @return the result.
   * @see #canBuildPostCache(User, Block)
   */
  public boolean canBuild(@NonNull User user, @NonNull Block block) {
    UUID uuid = user.uuid();
    return cache.computeIfAbsent(uuid, u -> buildCache()).get(block, b -> canBuildPostCache(user, b));
  }

  /**
   * Checks if a user can build at a block location.
   * @param user the user to check
   * @param block the block to check
   * @return true if all enabled protections allow it, false otherwise
   */
  private boolean canBuildPostCache(User user, Block block) {
    return Registries.PROTECTIONS.stream().allMatch(m -> m.canBuild(user.entity(), block));
  }

  /**
   * Creates a block cache in which entries expire 5000ms after their last access time.
   * @return the created cache
   * @see Caffeine
   */
  private static Cache<Block, Boolean> buildCache() {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofMillis(5000)).build();
  }
}
