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
import java.net.URI;
import java.nio.file.Path;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.sponge.SpongeCommandManager;
import com.google.inject.Inject;
import me.moros.bending.command.BendingCommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.game.GameImpl;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.PlaceholderListener;
import me.moros.bending.listener.PlayerListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.platform.CommandSender;
import me.moros.bending.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.platform.Platform;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.SpongePlatform;
import me.moros.bending.platform.world.SpongeWorldManager;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.metadata.Metadata;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.sponge.SpongeExecutor;
import org.bstats.sponge.Metrics;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
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
  private final Path dir;

  private final ConfigManager configManager;
  private final TranslationManager translationManager;

  private final BendingStorage storage;
  private Game game;

  private final boolean loaded;

  @Inject
  public SpongeBending(@ConfigDir(sharedRoot = false) Path dir, PluginContainer container, Metrics.Factory metricsFactory) {
    this.dir = dir;
    this.container = container;
    metricsFactory.make(8717);
    this.logger = container.logger(); // TODO SLF4J not currently available in sponge

    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);

    Tasker.inject(CompositeExecutor.of(new SpongeExecutor(container)));
    storage = StorageFactory.createInstance(this, dir);
    if (storage != null) {
      loaded = true;
      new AbilityInitializer();
      BendingProperties.inject(ConfigManager.load(BendingPropertiesImpl::new));
    } else {
      loaded = false;
      logger.error("Unable to establish database connection!");
    }
  }

  @Listener
  public void onGameLoad(LoadedGameEvent event) { // Plugins, Game-scoped registries are ready
    if (loaded) {
      SpongeCommandManager<CommandSender> manager = new SpongeCommandManager<>(
        container, CommandExecutionCoordinator.simpleCoordinator(),
        CommandSender::cause, CommandSender::from
      );
      new BendingCommandManager<>(this, game, PlayerCommandSender.class, manager);
      event.game().eventManager().registerListeners(container, new PlaceholderListener());
      if (event.game().pluginManager().plugin("LuckPerms").isPresent()) {
        new LuckPermsHook(event.game().serviceProvider());
      }
    }
  }

  @Listener
  public void onEnable(StartedEngineEvent<Server> event) { // Worlds have been loaded
    if (loaded) {
      Platform.inject(new SpongePlatform(dir, this));
      game = new GameImpl(this, storage);
      configManager.save();
      var eventManager = event.game().eventManager();
      eventManager.registerListeners(container, new BlockListener(game));
      eventManager.registerListeners(container, new EntityListener(game));
      eventManager.registerListeners(container, new PlayerListener(game, this));
      eventManager.registerListeners(container, new WorldListener(game, event.game().server()));
      GameProvider.register(game);
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
      SpongeWorldManager.INSTANCE.cleanup();
      game.cleanup(true);
      GameProvider.unregister();
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
  public ConfigManager configManager() {
    return configManager;
  }

  @Override
  public TranslationManager translationManager() {
    return translationManager;
  }

  @Override
  public @Nullable InputStream resource(String fileName) {
    return container.openResource(URI.create(fileName)).orElse(null);
  }
}
