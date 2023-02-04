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

package me.moros.bending.sponge;

import java.io.InputStream;
import java.nio.file.Path;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.sponge.SpongeCommandManager;
import com.google.inject.Inject;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.metadata.Metadata;
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
import me.moros.bending.sponge.hook.LuckPermsHook;
import me.moros.bending.sponge.listener.BlockListener;
import me.moros.bending.sponge.listener.ConnectionListener;
import me.moros.bending.sponge.listener.PlaceholderListener;
import me.moros.bending.sponge.listener.UserListener;
import me.moros.bending.sponge.listener.WorldListener;
import me.moros.bending.sponge.platform.CommandSender;
import me.moros.bending.sponge.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.SpongePermissionInitializer;
import me.moros.bending.sponge.platform.SpongePlatform;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.sponge.SpongeExecutor;
import org.bstats.sponge.Metrics;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("bending")
public class SpongeBending implements BendingPlugin {
  private final PluginContainer container;
  private final Logger logger;

  private final ConfigManager configManager;
  private final TranslationManager translationManager;

  private final BendingStorage storage;
  private Game game;

  private final boolean loaded;

  @Inject
  public SpongeBending(@ConfigDir(sharedRoot = false) Path dir, PluginContainer container, Metrics.Factory metricsFactory) {
    this.container = container;
    metricsFactory.make(8717);
    this.logger = LoggerFactory.getLogger("bending");

    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);

    storage = StorageFactory.createInstance(this, dir);
    if (storage != null) {
      loaded = true;
      new AbilityInitializer();
      SpongeCommandManager<CommandSender> manager = new SpongeCommandManager<>(
        container, CommandExecutionCoordinator.simpleCoordinator(),
        CommandSender::cause, CommandSender::from
      );
      new BendingCommandManager<>(this, PlayerCommandSender.class, manager);
    } else {
      loaded = false;
      logger.error("Unable to establish database connection!");
    }
  }

  @Listener
  public void onEnable(StartedEngineEvent<Server> event) { // Worlds have been loaded
    if (loaded) {
      ReflectionUtil.injectStatic(Tasker.class, CompositeExecutor.of(new SpongeExecutor(container)));
      ReflectionUtil.injectStatic(Platform.Holder.class, new SpongePlatform());
      ReflectionUtil.injectStatic(BendingProperties.Holder.class, ConfigManager.load(BendingPropertiesImpl::new));
      game = new GameImpl(this, storage);
      new SpongePermissionInitializer();
      var eventManager = event.game().eventManager();
      eventManager.registerListeners(container, new BlockListener(game));
      eventManager.registerListeners(container, new UserListener(game, this));
      eventManager.registerListeners(container, new ConnectionListener(game, this));
      eventManager.registerListeners(container, new WorldListener(game));
      eventManager.registerListeners(container, new PlaceholderListener());
      GameProviderUtil.registerProvider(game);
    }
  }

  @Listener
  public void onGameLoad(LoadedGameEvent event) { // Plugins, Game-scoped registries are ready
    if (loaded) {
      if (event.game().pluginManager().plugin("LuckPerms").isPresent()) {
        LuckPermsHook.register(event.game().serviceProvider());
      }
    }
  }

  @Listener
  public void onReload(RefreshGameEvent event) {
    if (game != null) {
      game.reload();
    }
  }

  @Listener
  public void onDisable(StoppingEngineEvent<Server> event) {
    if (game != null) {
      game.cleanup(true);
      GameProviderUtil.unregisterProvider();
      game = null;
    }
  }

  @Listener
  public void onServiceProvide(ProvideServiceEvent<Game> event) {
    if (game != null) {
      event.suggest(() -> game);
    }
  }

  @Listener
  public void onRegisterData(RegisterDataEvent event) {
    event.register(DataRegistration.of(PlatformAdapter.dataKey(Metadata.ARMOR_KEY), ItemStack.class));
    event.register(DataRegistration.of(PlatformAdapter.dataKey(Metadata.METAL_KEY), ItemStack.class));
  }

  @Override
  public String author() {
    return container.metadata().contributors().get(0).name();
  }

  @Override
  public String version() {
    return container.metadata().version().toString();
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
    return getClass().getClassLoader().getResourceAsStream(fileName);
  }
}
