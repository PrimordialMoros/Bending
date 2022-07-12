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

package me.moros.bending;

import java.util.Map;
import java.util.function.Function;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.protection.Protection;
import me.moros.bending.protection.plugin.GriefPreventionProtection;
import me.moros.bending.protection.plugin.LWCProtection;
import me.moros.bending.protection.plugin.TownyProtection;
import me.moros.bending.protection.plugin.WorldGuardProtection;
import me.moros.bending.registry.Registries;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.spongepowered.configurate.CommentedConfigurationNode;

/**
 * Used to initialize all default protections
 */
public final class ProtectionInitializer {
  private final PluginManager manager;
  private final CommentedConfigurationNode config;

  ProtectionInitializer(PluginManager manager, ConfigManager configManager) {
    this.manager = manager;
    config = configManager.config();
    Map<String, Function<Plugin, Protection>> map = Map.of(
      "WorldGuard", WorldGuardProtection::new,
      "GriefPrevention", GriefPreventionProtection::new,
      "Towny", TownyProtection::new,
      "LWC", LWCProtection::new
    );
    map.forEach(this::tryRegisterProtection);
  }

  private void tryRegisterProtection(String name, Function<Plugin, Protection> factory) {
    if (config.node("protection", name).getBoolean(true)) {
      Plugin plugin = manager.getPlugin(name);
      if (plugin != null && plugin.isEnabled()) {
        Protection protection = factory.apply(plugin);
        Registries.PROTECTIONS.register(protection);
      }
    }
  }
}
