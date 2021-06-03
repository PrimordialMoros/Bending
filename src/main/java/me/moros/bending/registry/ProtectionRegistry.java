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

package me.moros.bending.registry;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.PluginNotFoundException;
import me.moros.bending.protection.instances.GriefPreventionProtection;
import me.moros.bending.protection.instances.Protection;
import me.moros.bending.protection.instances.TownyProtection;
import me.moros.bending.protection.instances.WorldGuardProtection;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents the protection system which hooks into other region protection plugins.
 */
public final class ProtectionRegistry implements Registry<Protection> {
  /**
   * A multi-layered cache used to check if a User can build in a specific block location.
   * While this implementation is thread-safe it might be dangerous to use this async as the protection plugins
   * might not be thread-safe themselves and we load data from them when results aren't cached.
   */
  private final Map<UUID, Cache<Block, Boolean>> cache;
  private final Map<String, Protection> protections;

  ProtectionRegistry() {
    cache = new ConcurrentHashMap<>();
    protections = new ConcurrentHashMap<>();
  }

  public void init() {
    register("WorldGuard", WorldGuardProtection::new);
    register("GriefPrevention", GriefPreventionProtection::new);
    register("Towny", TownyProtection::new);
  }

  /**
   * Check if there is a proection method registered for the specified key.
   * @param name the protection name
   * @return true if that protection is registered, false otherwise
   */
  public boolean contains(@NonNull String name) {
    return protections.containsKey(name);
  }

  /**
   * Register a new {@link Protection}
   * @param name the name of the protection to register
   * @param creator the factory function that creates the protection instance
   */
  public void register(@NonNull String name, @NonNull ProtectionFactory creator) {
    if (Bending.configManager().config().node("protection", name).getBoolean(true)) {
      try {
        Protection method = creator.create();
        protections.put(name, method);
        Bending.logger().info("Registered bending protection for " + name);
      } catch (PluginNotFoundException e) {
        Bending.logger().warn("ProtectMethod " + name + " not able to be used since plugin was not found.");
      }
    }
  }

  /**
   * Remove the block protection cache for the specified user.
   * @param user the user for which the cache will be invalidated
   */
  public void invalidate(@NonNull User user) {
    cache.remove(user.entity().getUniqueId());
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
    UUID uuid = user.entity().getUniqueId();
    return cache.computeIfAbsent(uuid, u -> buildCache()).get(block, b -> canBuildPostCache(user, b));
  }

  /**
   * Checks if a user can build at a block location.
   * @param user the user to check
   * @param block the block to check
   * @return true if all enabled protections allow it, false otherwise
   */
  private boolean canBuildPostCache(User user, Block block) {
    return protections.values().stream().allMatch(m -> m.canBuild(user.entity(), block));
  }

  /**
   * Creates a block cache in which entries expire 5000ms after their last access time.
   * @return the created cache
   * @see Caffeine
   */
  private static Cache<Block, Boolean> buildCache() {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofMillis(5000)).build();
  }

  @Override
  public @NonNull Iterator<Protection> iterator() {
    return Collections.unmodifiableCollection(protections.values()).iterator();
  }

  @FunctionalInterface
  public interface ProtectionFactory {
    @NonNull Protection create() throws PluginNotFoundException;
  }
}
