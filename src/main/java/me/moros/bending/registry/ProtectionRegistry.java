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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import me.moros.bending.Bending;
import me.moros.bending.protection.PluginNotFoundException;
import me.moros.bending.protection.instances.GriefPreventionProtection;
import me.moros.bending.protection.instances.Protection;
import me.moros.bending.protection.instances.TownyProtection;
import me.moros.bending.protection.instances.WorldGuardProtection;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents the protection system which hooks into other region protection plugins.
 */
public final class ProtectionRegistry implements Registry<Protection> {

  private final Map<String, Protection> protections;

  ProtectionRegistry() {
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

  public @NonNull Stream<Protection> stream() {
    return protections.values().stream();
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
