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

package me.moros.bending.fabric;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricServerCommandManager;
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
import me.moros.bending.fabric.hook.LuckPermsHook;
import me.moros.bending.fabric.hook.PlaceholderHook;
import me.moros.bending.fabric.listener.BlockListener;
import me.moros.bending.fabric.listener.ConnectionListener;
import me.moros.bending.fabric.listener.UserListener;
import me.moros.bending.fabric.listener.WorldListener;
import me.moros.bending.fabric.platform.AbilityDamageSource;
import me.moros.bending.fabric.platform.CommandSender;
import me.moros.bending.fabric.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.fabric.platform.FabricMetadata;
import me.moros.bending.fabric.platform.FabricPlatform;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.fabric.FabricExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricBending implements BendingPlugin {
  private final ModContainer container;
  private final Logger logger;

  private final ConfigManager configManager;
  private final TranslationManager translationManager;

  private final BendingStorage storage;
  private LoadPhase phase = LoadPhase.FIRST;
  private Game game;
  private Collection<Object> listeners;

  public FabricBending(Path dir, ModContainer container) {
    this.container = container;
    this.logger = LoggerFactory.getLogger(container.getMetadata().getName());

    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);

    storage = StorageFactory.createInstance(this, dir);
    if (storage != null) {
      ReflectionUtil.injectStatic(Tasker.class, CompositeExecutor.of(new FabricExecutor()));
      ReflectionUtil.injectStatic(BendingProperties.Holder.class, ConfigManager.load(BendingPropertiesImpl::new));
      ReflectionUtil.injectStatic(AbilityDamageSource.class, translationManager);

      Tasker.async().repeat(FabricMetadata.INSTANCE::removeEmpty, 5, TimeUnit.MINUTES);

      registerLifecycleListeners();
      new AbilityInitializer();
      CommandManager<CommandSender> manager = new FabricServerCommandManager<>(
        CommandExecutionCoordinator.simpleCoordinator(),
        CommandSender::from, CommandSender::stack
      );
      new BendingCommandManager<>(this, PlayerCommandSender.class, manager);
    } else {
      phase = LoadPhase.FAIL;
      logger.error("Unable to establish database connection!");
    }
  }

  private void registerLifecycleListeners() {
    ServerLifecycleEvents.SERVER_STARTED.register(this::onEnable);
    ServerLifecycleEvents.SERVER_STOPPED.register(s -> onDisable(s.isDedicatedServer()));
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      ClientLifecycleEvents.CLIENT_STOPPING.register(m -> onDisable(true));
    }
  }

  private void onEnable(MinecraftServer server) {
    if (phase == LoadPhase.FIRST) {
      new PlaceholderHook().init();
      if (FabricLoader.getInstance().isModLoaded("LuckPerms")) {
        LuckPermsHook.register();
      }
      phase = LoadPhase.LOADING;
    }
    if (phase == LoadPhase.LOADING) {
      ReflectionUtil.injectStatic(Platform.Holder.class, new FabricPlatform(server));
      game = new GameImpl(this, storage);
      GameProviderUtil.registerProvider(game);
      phase = LoadPhase.LOADED;
    }

    if (listeners == null) {
      listeners = List.of(
        new BlockListener(this::game),
        new UserListener(this::game),
        new ConnectionListener(this::game, this, storage),
        new WorldListener(this::game)
      );
    }
  }

  private void onDisable(boolean fullShutdown) {
    if (phase == LoadPhase.LOADED) {
      FabricMetadata.INSTANCE.cleanup();
      game.cleanup(fullShutdown);
      GameProviderUtil.unregisterProvider();
      game = null;
      phase = LoadPhase.LOADING;
    }
  }

  private Game game() {
    return Objects.requireNonNull(game, "Trying to access game while it's not loaded!");
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

  private enum LoadPhase {FIRST, LOADING, LOADED, FAIL}
}
