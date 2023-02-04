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

package me.moros.bending.paper;

import java.io.InputStream;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.BendingPlugin;
import me.moros.bending.common.GameProviderUtil;
import me.moros.bending.common.ability.AbilityInitializer;
import me.moros.bending.common.command.BendingCommandManager;
import me.moros.bending.common.config.BendingPropertiesImpl;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.game.GameImpl;
import me.moros.bending.common.locale.TranslationManager;
import me.moros.bending.common.storage.StorageFactory;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.paper.hook.LuckPermsHook;
import me.moros.bending.paper.hook.PlaceholderAPIHook;
import me.moros.bending.paper.listener.BlockListener;
import me.moros.bending.paper.listener.ConnectionListener;
import me.moros.bending.paper.listener.UserListener;
import me.moros.bending.paper.listener.WorldListener;
import me.moros.bending.paper.platform.BukkitPermissionInitializer;
import me.moros.bending.paper.platform.BukkitPlatform;
import me.moros.bending.paper.protection.ProtectionInitializer;
import me.moros.bending.paper.protection.WorldGuardFlag;
import me.moros.tasker.bukkit.BukkitExecutor;
import me.moros.tasker.executor.CompositeExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class PaperBending extends JavaPlugin implements BendingPlugin {
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
    if (storage == null) {
      logger.error("Unable to establish database connection!");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    new Metrics(this, 8717);
    ReflectionUtil.injectStatic(Tasker.class, CompositeExecutor.of(new BukkitExecutor(this)));
    ReflectionUtil.injectStatic(Platform.Holder.class, new BukkitPlatform(logger));
    ReflectionUtil.injectStatic(BendingProperties.Holder.class, ConfigManager.load(BendingPropertiesImpl::new));
    new ProtectionInitializer(this);
    game = new GameImpl(this, storage);
    new BukkitPermissionInitializer();

    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new UserListener(game, this), this);
    getServer().getPluginManager().registerEvents(new ConnectionListener(game, this), this);
    getServer().getPluginManager().registerEvents(new WorldListener(game), this);

    try {
      PaperCommandManager<CommandSender> manager = PaperCommandManager.createNative(this, CommandExecutionCoordinator.simpleCoordinator());
      manager.registerAsynchronousCompletions();
      new BendingCommandManager<>(this, Player.class, manager);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    getServer().getServicesManager().register(Game.class, game, this, ServicePriority.Normal);
    GameProviderUtil.registerProvider(game);
    registerHooks();
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup(true);
      GameProviderUtil.unregisterProvider();
      game = null;
    }
  }

  private void registerHooks() {
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PlaceholderAPIHook(this).register();
    }
    if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
      LuckPermsHook.register(getServer().getServicesManager());
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
  public void reload() {
    if (game != null) {
      game.reload();
    }
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
}
