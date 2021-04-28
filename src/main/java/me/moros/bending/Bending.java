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

package me.moros.bending;

import java.util.Objects;

import me.moros.atlas.acf.lib.timings.TimingManager;
import me.moros.bending.command.Commands;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.events.BendingEventBus;
import me.moros.bending.game.Game;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.UserListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.PersistentDataLayer;
import me.moros.bending.util.Tasker;
import me.moros.storage.logging.Logger;
import me.moros.storage.logging.Slf4jLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

public class Bending extends JavaPlugin {
  private static Bending plugin;

  private Logger logger;

  private ConfigManager configManager;
  private TranslationManager translationManager;
  private TimingManager timingManager;

  private PersistentDataLayer layer;
  private BendingEventBus eventBus;
  private Game game;

  private String author;
  private String version;

  @Override
  public void onEnable() {
    new Metrics(this, 8717);
    plugin = this;
    logger = new Slf4jLogger(LoggerFactory.getLogger(getClass().getSimpleName()));
    author = getDescription().getAuthors().get(0);
    version = getDescription().getVersion();

    Tasker.init(this);

    String dir = getConfigFolder();

    configManager = new ConfigManager(dir);
    translationManager = new TranslationManager(dir);
    timingManager = TimingManager.of(this);
    eventBus = new BendingEventBus(this);
    layer = new PersistentDataLayer(this);

    BendingStorage storage = Objects.requireNonNull(StorageFactory.createInstance(), "Unable to connect to database!");
    game = new Game(storage);
    configManager.save();

    getServer().getPluginManager().registerEvents(new WorldListener(game), this);
    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new EntityListener(game), this);
    getServer().getPluginManager().registerEvents(new UserListener(game), this);

    new Commands(this, game);
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup();
    }
    getServer().getScheduler().cancelTasks(this);
  }

  @Override
  public void onLoad() {
    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
      WorldGuardFlag.registerFlag();
    }
  }

  public static BendingEventBus getEventBus() {
    return plugin.eventBus;
  }

  public static TimingManager getTimingManager() {
    return plugin.timingManager;
  }

  public static Bending getPlugin() {
    return plugin;
  }

  public static ConfigManager getConfigManager() {
    return plugin.configManager;
  }

  public static TranslationManager getTranslationManager() {
    return plugin.translationManager;
  }

  public static String getAuthor() {
    return plugin.author;
  }

  public static String getVersion() {
    return plugin.version;
  }

  public static Logger getLog() {
    return plugin.logger;
  }

  public static PersistentDataLayer getLayer() {
    return plugin.layer;
  }

  public static Game getGame() {
    return plugin.game;
  }

  public static String getConfigFolder() {
    return plugin.getDataFolder().toString();
  }
}
