/*
 * Copyright 2020-2025 Moros
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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.common.AbstractBending;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.hook.LuckPermsHook;
import me.moros.bending.common.hook.MiniPlaceholdersHook;
import me.moros.bending.common.hook.PresetLimits;
import me.moros.bending.common.logging.Slf4jLogger;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.fabric.game.DummyGame;
import me.moros.bending.fabric.hook.PlaceholderHook;
import me.moros.bending.fabric.listener.BlockListener;
import me.moros.bending.fabric.listener.ConnectionListener;
import me.moros.bending.fabric.listener.UserListener;
import me.moros.bending.fabric.listener.WorldListener;
import me.moros.bending.fabric.platform.BrigadierSetup;
import me.moros.bending.fabric.platform.CommandSender;
import me.moros.bending.fabric.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.fabric.platform.FabricMetadata;
import me.moros.bending.fabric.platform.FabricPermissionInitializer;
import me.moros.bending.fabric.platform.FabricPlatform;
import me.moros.tasker.fabric.FabricExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.slf4j.LoggerFactory;

final class FabricBending extends AbstractBending<ModContainer> {
  private LoadPhase phase = LoadPhase.FIRST;
  private final Collection<Initializer> listeners;
  private final AtomicBoolean exiting = new AtomicBoolean();

  FabricBending(ModContainer container, Path path) {
    super(container, path, new Slf4jLogger(LoggerFactory.getLogger(container.getMetadata().getName())));

    injectTasker(new FabricExecutor());

    Tasker.async().repeat(FabricMetadata.INSTANCE::removeEmpty, 5, TimeUnit.MINUTES);

    listeners = List.of(
      new BlockListener(this::game),
      new UserListener(this::game),
      new ConnectionListener(logger(), this::game),
      new WorldListener(this::game)
    );
    registerLifecycleListeners();

    var manager = new FabricServerCommandManager<>(
      ExecutionCoordinator.simpleCoordinator(),
      SenderMapper.create(CommandSender::from, CommandSender::stack)
    );
    BrigadierSetup.setup(manager);
    Commander.create(manager, PlayerCommandSender.class, this).init();
  }

  private void registerLifecycleListeners() {
    ServerLifecycleEvents.SERVER_STARTED.register(this::onEnable);
    ServerLifecycleEvents.SERVER_STOPPING.register(s -> onDisable(exiting.get() || s.isDedicatedServer()));
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      ClientLifecycleEvents.CLIENT_STOPPING.register(m -> exiting.set(true));
    }
    new FabricPermissionInitializer().init();
  }

  private void onEnable(MinecraftServer server) {
    if (phase == LoadPhase.FIRST) {
      listeners.forEach(Initializer::init);
      registerAddonProviders();
      registerHooks();
      phase = LoadPhase.LOADING;
    }
    if (phase == LoadPhase.LOADING) {
      ReflectionUtil.injectStatic(Platform.Holder.class, new FabricPlatform(server));
      load();
      phase = LoadPhase.LOADED;
    }
  }

  private void onDisable(boolean fullShutdown) {
    if (phase == LoadPhase.LOADED) {
      FabricMetadata.INSTANCE.cleanup();
      if (fullShutdown) {
        disable();
      } else {
        softDisable();
      }
      phase = LoadPhase.LOADING;
    }
  }

  private void registerHooks() {
    if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
      new PlaceholderHook().init();
    }
    if (FabricLoader.getInstance().isModLoaded("miniplaceholders")) {
      new MiniPlaceholdersHook().init();
    }
    if (FabricLoader.getInstance().isModLoaded("luckperms")) {
      var hook = LuckPermsHook.register(ServerPlayer::getUUID);
      registerNamedAddon(PresetLimits.NAME, hook::presetLimits);
    }
  }

  private Game game() {
    return game != null ? game : DummyGame.INSTANCE;
  }

  @Override
  public String author() {
    return parent.getMetadata().getAuthors().stream().map(Person::getName).findFirst().orElse("Moros");
  }

  @Override
  public String version() {
    return parent.getMetadata().getVersion().getFriendlyString();
  }

  private void registerAddonProviders() {
    FabricLoader.getInstance().getEntrypointContainers("bending", Addon.class)
      .forEach(c -> registerNamedAddon(c.getProvider().getMetadata().getId(), Suppliers.lazy(c::getEntrypoint)));
  }

  private enum LoadPhase {FIRST, LOADING, LOADED}
}
