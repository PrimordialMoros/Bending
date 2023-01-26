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
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricServerCommandManager;
import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.command.BendingCommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.fabric.NativeAdapterImpl;
import me.moros.bending.game.GameImpl;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.hook.PlaceholderHook;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.ConnectionListener;
import me.moros.bending.listener.UserListener;
import me.moros.bending.listener.WorldListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.platform.AbilityDamageSource;
import me.moros.bending.platform.CommandSender;
import me.moros.bending.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.platform.FabricMetadata;
import me.moros.bending.platform.FabricPlatform;
import me.moros.bending.platform.Platform;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.fabric.FabricExecutor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricBending implements BendingPlugin {
  private static FabricAudiences audiences;

  private final ModContainer container;
  private final Logger logger;

  private final ConfigManager configManager;
  private final TranslationManager translationManager;

  private final BendingStorage storage;
  private Game game;
  private Collection<Object> listeners;

  private final boolean loaded;

  public FabricBending(Path dir, ModContainer container) {
    this.container = container;
    this.logger = LoggerFactory.getLogger(container.getMetadata().getName());

    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);
    AbilityDamageSource.inject(s -> translationManager.translate(s) != null);
    Tasker.inject(CompositeExecutor.of(new FabricExecutor()));
    registerLifecycleListeners();
    storage = StorageFactory.createInstance(this, dir);
    if (storage != null) {
      loaded = true;
      new AbilityInitializer();
      BendingProperties.inject(ConfigManager.load(BendingPropertiesImpl::new));
      CommandManager<CommandSender> manager = new FabricServerCommandManager<>(
        CommandExecutionCoordinator.simpleCoordinator(),
        CommandSender::from, CommandSender::stack
      );
      new BendingCommandManager<>(this, game, PlayerCommandSender.class, manager);
    } else {
      loaded = false;
      logger.error("Unable to establish database connection!");
    }
  }

  private void registerLifecycleListeners() {
    ServerLifecycleEvents.SERVER_STARTED.register(this::onEnable);
    ServerLifecycleEvents.SERVER_STOPPED.register(this::onDisable);
  }

  private void onEnable(MinecraftServer server) {
    if (loaded) {
      audiences = FabricServerAudiences.of(server);
      new PlaceholderHook().init();
      if (FabricLoader.getInstance().isModLoaded("LuckPerms")) {
        LuckPermsHook.register();
      }
      NativeAdapter.inject(new NativeAdapterImpl(server, audiences));
      Platform.inject(new FabricPlatform(server));
      game = new GameImpl(this, storage);
      listeners = List.of(
        new BlockListener(game),
        new UserListener(game),
        new ConnectionListener(game, this),
        new WorldListener(game)
      );
      GameProvider.register(game);
    }
  }

  private void onDisable(MinecraftServer server) {
    if (game != null) {
      FabricMetadata.INSTANCE.cleanup();
      game.cleanup(true);
      GameProvider.unregister();
      game = null;
    }
  }

  @Override
  public String author() {
    return container.getMetadata().getAuthors().stream().map(Person::getName).findFirst().orElse("Moros");
  }

  @Override
  public String version() {
    return container.getMetadata().getVersion().getFriendlyString();
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
    return getClass().getClassLoader().getResourceAsStream(fileName);
  }

  public static @MonotonicNonNull FabricAudiences audiences() {
    return audiences;
  }
}
