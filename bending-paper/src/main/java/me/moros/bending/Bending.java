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

import java.util.Objects;

import me.moros.bending.adapter.VersionUtil;
import me.moros.bending.adapter.impl.NativeAdapter;
import me.moros.bending.command.CommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.game.GameImpl;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.hook.placeholder.BendingExpansion;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.UserListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.protection.plugin.GriefPreventionProtection;
import me.moros.bending.protection.plugin.LWCProtection;
import me.moros.bending.protection.plugin.TownyProtection;
import me.moros.bending.protection.plugin.WorldGuardProtection;
import me.moros.bending.registry.ProtectionRegistry.ProtectionFactory;
import me.moros.bending.registry.Registries;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.metadata.Metadata;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

public class Bending extends JavaPlugin {
  private static Bending plugin;

  private Logger logger;
  private String author;
  private String version;

  private ConfigManager configManager;
  private TranslationManager translationManager;

  private Game game;

  @Override
  public void onEnable() {
    new Metrics(this, 8717);
    plugin = this;
    logger = getSLF4JLogger();
    author = getDescription().getAuthors().get(0);
    version = getDescription().getVersion();

    loadAdapter();
    String dir = plugin.getDataFolder().toString();
    configManager = new ConfigManager(dir);
    translationManager = new TranslationManager(dir);

    BendingProperties.inject(new BendingPropertiesImpl());
    Metadata.init(this);
    Tasker.INSTANCE.init(this);
    BendingStorage storage = Objects.requireNonNull(StorageFactory.createInstance(dir), "Unable to connect to database!");
    game = new GameImpl(storage);

    getServer().getPluginManager().registerEvents(new WorldListener(game), this);
    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new EntityListener(game), this);
    getServer().getPluginManager().registerEvents(new UserListener(game), this);

    try {
      new CommandManager(this);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    registerHooks();
    configManager.save();
    getServer().getServicesManager().register(Game.class, game, this, ServicePriority.Normal);
  }

  private void loadAdapter() {
    NativeAdapter.init();
    if (NativeAdapter.instance().isNative()) {
      logger.info("Successfully loaded native adapter for version " + VersionUtil.nmsVersion());
    } else {
      String s = String.format("""
                
        ****************************************************************
        * Unable to find native adapter for version %s.
        * Some features may be unsupported (for example toast notifications) or induce significant overhead.
        * Packet based abilities will utilize real entities instead which can be slower when spawned in large amounts.
        * It is recommended you find a supported version.
        ****************************************************************
                
        """, VersionUtil.nmsVersion());
      logger.warn(s);
    }
  }

  private void registerHooks() {
    tryRegisterProtection("WorldGuard", WorldGuardProtection::new);
    tryRegisterProtection("GriefPrevention", GriefPreventionProtection::new);
    tryRegisterProtection("Towny", TownyProtection::new);
    tryRegisterProtection("LWC", LWCProtection::new);

    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new BendingExpansion().register();
    }
    if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
      new LuckPermsHook(this);
    }
  }

  private void tryRegisterProtection(String name, ProtectionFactory factory) {
    if (Bending.configManager().config().node("protection", name).getBoolean(true)) {
      if (Registries.PROTECTIONS.register(name, factory)) {
        Bending.logger().info("Registered bending protection for " + name);
      } else {
        Bending.logger().warn("Unable to register bending protection for " + name);
      }
    }
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup(true);
    }
  }

  @Override
  public void onLoad() {
    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
      WorldGuardFlag.registerFlag();
    }
  }

  public static @MonotonicNonNull Bending plugin() {
    return plugin;
  }

  public static @MonotonicNonNull ConfigManager configManager() {
    return plugin.configManager;
  }

  public static @MonotonicNonNull TranslationManager translationManager() {
    return plugin.translationManager;
  }

  public static @MonotonicNonNull String author() {
    return plugin.author;
  }

  public static @MonotonicNonNull String version() {
    return plugin.version;
  }

  public static @MonotonicNonNull Logger logger() {
    return plugin.logger;
  }

  public static @MonotonicNonNull Game game() {
    return plugin.game;
  }
}
