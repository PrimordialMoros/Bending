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

import java.nio.file.Path;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.sponge.SpongeCommandManager;
import com.google.inject.Inject;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.common.AbstractBending;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.sponge.gui.ElementMenu;
import me.moros.bending.sponge.hook.LuckPermsHook;
import me.moros.bending.sponge.listener.BlockListener;
import me.moros.bending.sponge.listener.ConnectionListener;
import me.moros.bending.sponge.listener.PlaceholderListener;
import me.moros.bending.sponge.listener.UserListener;
import me.moros.bending.sponge.listener.WorldListener;
import me.moros.bending.sponge.platform.AbilityDamageSource;
import me.moros.bending.sponge.platform.CommandSender;
import me.moros.bending.sponge.platform.CommandSender.PlayerCommandSender;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.SpongePermissionInitializer;
import me.moros.bending.sponge.platform.SpongePlatform;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.sponge.SpongeExecutor;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.datapack.DataPacks;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageScalings;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.DamageTypeTemplate;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.tag.DamageTypeTags;
import org.spongepowered.api.tag.TagTemplate;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("bending")
public final class SpongeBending extends AbstractBending<PluginContainer> {
  @Inject
  public SpongeBending(PluginContainer container, @ConfigDir(sharedRoot = false) Path dir, Metrics.Factory metricsFactory) {
    super(container, dir, new LoggerImpl(container.logger()));
    metricsFactory.make(8717);

    SpongeCommandManager<CommandSender> manager = new SpongeCommandManager<>(
      container, CommandExecutionCoordinator.simpleCoordinator(),
      CommandSender::cause, CommandSender::from
    );
    Commander.create(manager, PlayerCommandSender.class, this).init();
  }

  @Listener
  public void onEnable(StartedEngineEvent<Server> event) { // Worlds have been loaded
    ReflectionUtil.injectStatic(ElementMenu.class, parent);
    ReflectionUtil.injectStatic(Tasker.class, CompositeExecutor.of(new SpongeExecutor(parent)));
    ReflectionUtil.injectStatic(Platform.Holder.class, new SpongePlatform());
    load();
    new SpongePermissionInitializer().init();
    var eventManager = event.game().eventManager();
    eventManager.registerListeners(parent, new BlockListener(game));
    eventManager.registerListeners(parent, new UserListener(game));
    eventManager.registerListeners(parent, new ConnectionListener(game, this));
    eventManager.registerListeners(parent, new WorldListener(game));
    eventManager.registerListeners(parent, new PlaceholderListener());
  }

  @Listener
  public void onGameLoad(LoadedGameEvent event) { // Plugins, Game-scoped registries are ready
    if (game != null && event.game().pluginManager().plugin("LuckPerms").isPresent()) {
      LuckPermsHook.register(event.game().serviceProvider());
    }
  }

  @Listener
  public void onReload(RefreshGameEvent event) {
    reload();
  }

  @Listener
  public void onDisable(StoppingEngineEvent<Server> event) {
    disable();
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

  @Listener
  private void onDamageTypePack(RegisterDataPackValueEvent<DamageTypeTemplate> event) {
    var template = DamageTypeTemplate.builder().name("bending-damage").scaling(DamageScalings.NEVER.get())
      .exhaustion(0).key(AbilityDamageSource.BENDING_DAMAGE).build();
    event.register(template);
  }

  @Listener
  private void onDamageTypeTagPack(RegisterDataPackValueEvent<TagTemplate<DamageType>> event) {
    var template = TagTemplate.builder(DataPacks.DAMAGE_TYPE_TAG).key(DamageTypeTags.BYPASSES_INVULNERABILITY.key())
      .addValue(RegistryKey.of(RegistryTypes.DAMAGE_TYPE, AbilityDamageSource.BENDING_DAMAGE)).build();
    event.register(template);
  }

  @Override
  public String author() {
    return parent.metadata().contributors().get(0).name();
  }

  @Override
  public String version() {
    return parent.metadata().version().toString();
  }
}
