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

import me.moros.bending.command.CommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.event.BendingEventBus;
import me.moros.bending.game.Game;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.hook.placeholder.BendingExpansion;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.UserListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.metadata.PersistentDataLayer;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

public class Bending extends JavaPlugin {
  private static Bending plugin;

  private Logger logger;

  private ConfigManager configManager;
  private TranslationManager translationManager;

  private PersistentDataLayer dataLayer;
  private BendingEventBus eventBus;
  private Game game;

  private String author;
  private String version;

  @Override
  public void onEnable() {
    new Metrics(this, 8717);
    plugin = this;
    logger = getSLF4JLogger();
    author = getDescription().getAuthors().get(0);
    version = getDescription().getVersion();

    String dir = configFolder();

    configManager = new ConfigManager(dir);
    translationManager = new TranslationManager(dir);
    eventBus = new BendingEventBus(this);
    dataLayer = new PersistentDataLayer(this);

    BendingStorage storage = Objects.requireNonNull(StorageFactory.createInstance(), "Unable to connect to database!");
    game = new Game(storage);
    configManager.save();

    getServer().getPluginManager().registerEvents(new WorldListener(game), this);
    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new EntityListener(game), this);
    getServer().getPluginManager().registerEvents(new UserListener(game), this);

    try {
      new CommandManager(this);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      getServer().getPluginManager().disablePlugin(this);
    }

    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new BendingExpansion().register();
    }
    if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
      new LuckPermsHook(this);
    }
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup(true);
    }
    getServer().getScheduler().cancelTasks(this);
  }

  @Override
  public void onLoad() {
    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
      WorldGuardFlag.registerFlag();
    }
  }

  public static @MonotonicNonNull BendingEventBus eventBus() {
    return plugin.eventBus;
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

  public static @MonotonicNonNull PersistentDataLayer dataLayer() {
    return plugin.dataLayer;
  }

  public static @MonotonicNonNull Game game() {
    return plugin.game;
  }

  public static @MonotonicNonNull String configFolder() {
    return plugin.getDataFolder().toString();
  }
}
