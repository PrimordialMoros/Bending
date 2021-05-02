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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.exception.PluginNotFoundException;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.instances.GriefPreventionProtection;
import me.moros.bending.protection.instances.Protection;
import me.moros.bending.protection.instances.TownyProtection;
import me.moros.bending.protection.instances.WorldGuardProtection;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the protection system which hooks into other region protection plugins.
 */
public class ProtectionSystem {
  /**
   * A multi-layered cache used to check if a User can build in a specific block location.
   * While this implementation is thread-safe it might be dangerous to use this async as the protection plugins
   * might not be thread-safe themselves and we load data from them when results aren't cached.
   */
  private final Map<User, Cache<Block, Boolean>> cache = new ConcurrentHashMap<>();
  private final Collection<Protection> protections = new ArrayList<>();
  private final boolean allowHarmless;

  public ProtectionSystem() {
    allowHarmless = Bending.configManager().config().node("protection").node("allow-harmless").getBoolean(true);
    registerProtectMethod("WorldGuard", WorldGuardProtection::new);
    registerProtectMethod("GriefPrevention", GriefPreventionProtection::new);
    registerProtectMethod("Towny", TownyProtection::new);
  }

  /**
   * Remove the block protection cache for the specified user.
   * @param user the user to invalidate
   */
  public void invalidate(@NonNull User user) {
    cache.remove(user);
  }

  /**
   * @see #canBuild(User, Block, boolean)
   */
  public boolean canBuild(@NonNull User user, @NonNull Block block) {
    return canBuild(user, block, false);
  }

  /**
   * Uses {@link AbilityDescription#harmless}
   * @see #canBuild(User, Block, boolean)
   */
  public boolean canBuild(@NonNull User user, @NonNull Block block, @Nullable AbilityDescription desc) {
    return canBuild(user, block, desc != null && desc.harmless());
  }

  /**
   * Checks if a user can build at a block location. First it queries the cache.
   * If no result is found it computes it and adds it to the cache before returning the result.
   * Harmless actions are automatically allowed if allowHarmless is configured
   * @param user the user to check
   * @param block the block to check
   * @param isHarmless whether the action the user is peforming is harmless
   * @return the result.
   * @see #canBuildPostCache(User, Block)
   */
  public boolean canBuild(@NonNull User user, @NonNull Block block, boolean isHarmless) {
    if (isHarmless && allowHarmless) {
      return true;
    }
    return cache.computeIfAbsent(user, u -> buildCache()).get(block, b -> canBuildPostCache(user, b));
  }

  /**
   * Checks if a user can build at a block location.
   * @param user the user to check
   * @param block the block to check
   * @return true if all enabled protections allow it, false otherwise
   */
  private boolean canBuildPostCache(User user, Block block) {
    return protections.stream().allMatch(m -> m.canBuild(user, block));
  }

  /**
   * Register a new {@link Protection}
   * @param name the name of the protection to register
   * @param creator the factory function that creates the protection instance
   */
  public void registerProtectMethod(@NonNull String name, @NonNull ProtectionFactory creator) {
    if (Bending.configManager().config().node("protection", name).getBoolean(true)) {
      try {
        Protection method = creator.create();
        protections.add(method);
        Bending.logger().info("Registered bending protection for " + name);
      } catch (PluginNotFoundException e) {
        Bending.logger().warn("ProtectMethod " + name + " not able to be used since plugin was not found.");
      }
    }
  }

  /**
   * Creates a block cache in which entries expire 5000ms after their last access time.
   * @return the created cache
   * @see Caffeine
   */
  private static Cache<Block, Boolean> buildCache() {
    return Caffeine.newBuilder().expireAfterAccess(Duration.ofMillis(5000)).build();
  }

  @FunctionalInterface
  public interface ProtectionFactory {
    @NonNull Protection create() throws PluginNotFoundException;
  }
}
