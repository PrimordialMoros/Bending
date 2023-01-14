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

package me.moros.bending;

import java.io.InputStream;
import java.util.function.Function;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.command.BendingCommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.game.GameImpl;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.hook.PlaceholderAPIHook;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.PlayerListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.platform.BukkitPlatform;
import me.moros.bending.platform.Platform;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.tasker.bukkit.BukkitExecutor;
import me.moros.tasker.executor.CompositeExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class Bending extends JavaPlugin implements BendingPlugin {
  private static Bending plugin;
  private Logger logger;
  private String author;
  private String version;

  private ConfigManager configManager;
  private TranslationManager translationManager;

  private BendingStorage storage;
  private Game game;

  @Override
  public void onLoad() {
    logger = getSLF4JLogger();
    author = getDescription().getAuthors().get(0);
    version = getDescription().getVersion();

    var dir = getDataFolder().toPath();
    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);

    storage = StorageFactory.createInstance(this, dir);
    new AbilityInitializer();

    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
      WorldGuardFlag.registerFlag();
    }
  }

  @Override
  public void onEnable() {
    plugin = this;
    if (storage == null) {
      logger.error("Unable to establish database connection!");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    new Metrics(this, 8717);
    Tasker.inject(CompositeExecutor.of(new BukkitExecutor(this)));
    Platform.inject(new BukkitPlatform(this));
    loadAdapter();
    new ProtectionInitializer(this);
    BendingProperties.inject(ConfigManager.load(BendingPropertiesImpl::new));
    game = new GameImpl(this, storage);

    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new EntityListener(game), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this, game), this);
    getServer().getPluginManager().registerEvents(new WorldListener(game, getServer()), this);

    try {
      PaperCommandManager<CommandSender> manager = PaperCommandManager.createNative(this, CommandExecutionCoordinator.simpleCoordinator());
      manager.registerAsynchronousCompletions();
      new BendingCommandManager<>(this, game, Player.class, manager);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    configManager.save();
    getServer().getServicesManager().register(Game.class, game, this, ServicePriority.Normal);
    GameProvider.register(game);
    registerHooks();
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup(true);
      GameProvider.unregister();
      game = null;
    }
  }

  private @Nullable NativeAdapter findAdapter(String className) {
    try {
      Class<?> cls = Class.forName(className);
      if (!cls.isSynthetic() && NativeAdapter.class.isAssignableFrom(cls)) {
        Function<BlockState, BlockData> dataMapper = PlatformAdapter::toBukkitData;
        return (NativeAdapter) cls.getDeclaredConstructor(Function.class).newInstance(dataMapper);
      }
    } catch (Exception ignore) {
    }
    return null;
  }

  private void loadAdapter() {
    if (NativeAdapter.hasNativeSupport()) {
      logger.info("A native adapter has already been registered.");
      return;
    }
    String fullName = getServer().getClass().getPackageName();
    String nmsVersion = fullName.substring(1 + fullName.lastIndexOf("."));
    String className = "me.moros.bending.adapter.impl." + nmsVersion + ".NativeAdapterImpl";
    NativeAdapter adapter = findAdapter(className);
    if (adapter != null) {
      NativeAdapter.inject(adapter);
    }
    if (NativeAdapter.hasNativeSupport()) {
      logger.info("Successfully loaded native adapter for version " + nmsVersion);
    } else {
      String s = String.format("""
                
        ****************************************************************
        * Unable to find native adapter for version %s.
        * Some features may be unsupported (for example toast notifications) or induce significant overhead.
        * Packet based abilities will utilize real entities instead which can be slower when spawned in large amounts.
        * It is recommended you find a supported version.
        ****************************************************************
                
        """, nmsVersion);
      logger.warn(s);
    }
  }

  private void registerHooks() {
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PlaceholderAPIHook(this).register();
    }
    if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
      new LuckPermsHook(getServer().getServicesManager());
    }
  }

  @Override
  public String author() {
    return author;
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public ConfigManager configManager() {
    return configManager;
  }

  @Override
  public TranslationManager translationManager() {
    return translationManager;
  }

  @Override
  public @Nullable InputStream resource(String fileName) {
    return getResource(fileName);
  }

  public static @MonotonicNonNull Bending plugin() {
    return plugin;
  }
}
