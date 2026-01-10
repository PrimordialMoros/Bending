/*
 * Copyright 2020-2026 Moros
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.protection.Protection;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.paper.protection.plugin.BoltProtection;
import me.moros.bending.paper.protection.plugin.GriefPreventionProtection;
import me.moros.bending.paper.protection.plugin.TownyProtection;
import me.moros.bending.paper.protection.plugin.WorldGuardProtection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.objectmapping.meta.Setting;

public final class ProtectionInitializer implements Initializer {
  private final Config config;

  public ProtectionInitializer() {
    this.config = ConfigManager.load(Config::new);
  }

  @Override
  public void init() {
    Map<String, Function<Plugin, Protection>> map = Map.of(
      "WorldGuard", WorldGuardProtection::new,
      "GriefPrevention", GriefPreventionProtection::new,
      "Towny", TownyProtection::new,
      "Bolt", BoltProtection::new
    );
    map.forEach(this::tryRegisterProtection);
  }

  private void tryRegisterProtection(String name, Function<Plugin, Protection> factory) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
    if (plugin != null && plugin.isEnabled() && Boolean.TRUE.equals(config.protection.get(name))) {
      Protection protection = factory.apply(plugin);
      Registries.PROTECTIONS.register(protection);
    }
  }

  private static final class Config implements Configurable {
    @Setting(nodeFromParent = true)
    private final Map<String, Boolean> protection = Map.of(
      "WorldGuard", true,
      "GriefPrevention", true,
      "Towny", true,
      "Bolt", true
    );

    @Override
    public List<String> path() {
      return List.of("protection");
    }
  }
}
