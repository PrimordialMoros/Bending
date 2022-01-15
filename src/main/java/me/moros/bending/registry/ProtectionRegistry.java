/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.registry;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import me.moros.bending.Bending;
import me.moros.bending.protection.Protection;
import me.moros.bending.protection.plugin.GriefPreventionProtection;
import me.moros.bending.protection.plugin.LWCProtection;
import me.moros.bending.protection.plugin.TownyProtection;
import me.moros.bending.protection.plugin.WorldGuardProtection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Holds all the registered protection hooks for the current session.
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
    register("LWC", LWCProtection::new);
  }

  /**
   * Check if there is a protection method registered for the specified key.
   * @param name the protection name
   * @return true if that protection is registered, false otherwise
   */
  public boolean contains(@NonNull String name) {
    return protections.containsKey(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Register a new {@link Protection}
   * @param name the name of the protection to register
   * @param factory the factory function that creates the protection instance
   */
  public void register(@NonNull String name, @NonNull ProtectionFactory factory) {
    if (!contains(name) && Bending.configManager().config().node("protection", name).getBoolean(true)) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
      if (plugin != null) {
        Protection protection = factory.apply(plugin);
        protections.put(name.toLowerCase(Locale.ROOT), protection);
        Bending.logger().info("Registered bending protection for " + name);
      } else {
        Bending.logger().warn("Plugin " + name + " was not found, skipping protection hook");
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
  public interface ProtectionFactory extends Function<Plugin, Protection> {
    @NonNull Protection apply(@NonNull Plugin plugin);
  }
}
