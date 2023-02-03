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

package me.moros.bending.paper.protection;

import java.util.Map;
import java.util.function.Function;

import me.moros.bending.api.protection.Protection;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.BendingPlugin;
import me.moros.bending.common.platform.Initializer;
import me.moros.bending.paper.protection.plugin.GriefPreventionProtection;
import me.moros.bending.paper.protection.plugin.LWCProtection;
import me.moros.bending.paper.protection.plugin.TownyProtection;
import me.moros.bending.paper.protection.plugin.WorldGuardProtection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.CommentedConfigurationNode;

public final class ProtectionInitializer implements Initializer {
  private final CommentedConfigurationNode config;

  public ProtectionInitializer(BendingPlugin plugin) {
    this.config = plugin.configManager().config();
    init();
  }

  @Override
  public void init() {
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
      Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
      if (plugin != null && plugin.isEnabled()) {
        Protection protection = factory.apply(plugin);
        Registries.PROTECTIONS.register(protection);
      }
    }
  }
}
