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

package me.moros.bending.paper;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.AbstractBending;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.hook.LuckPermsHook;
import me.moros.bending.common.hook.MiniPlaceholdersHook;
import me.moros.bending.common.hook.PresetLimits;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.paper.hook.PlaceholderAPIHook;
import me.moros.bending.paper.listener.BlockListener;
import me.moros.bending.paper.listener.ConnectionListener;
import me.moros.bending.paper.listener.UserListener;
import me.moros.bending.paper.listener.WorldListener;
import me.moros.bending.paper.platform.BrigadierSetup;
import me.moros.bending.paper.platform.BukkitPermissionInitializer;
import me.moros.bending.paper.platform.BukkitPlatform;
import me.moros.tasker.paper.PaperExecutor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

final class PaperBending extends AbstractBending<BendingBootstrap> {
  PaperBending(BendingBootstrap parent, Path dir, Logger logger) {
    super(parent, dir, logger);
  }

  void onPluginEnable() {
    injectTasker(new PaperExecutor(parent));
    String version = parent.getPluginMeta().getAPIVersion();
    ReflectionUtil.injectStatic(Platform.Holder.class, new BukkitPlatform(logger(), version));
    registerHooks(parent.getServer());
    load();
    new BukkitPermissionInitializer().init();

    var pluginManager = parent.getServer().getPluginManager();
    pluginManager.registerEvents(new BlockListener(game), parent);
    pluginManager.registerEvents(new UserListener(game), parent);
    pluginManager.registerEvents(new ConnectionListener(logger(), game), parent);
    pluginManager.registerEvents(new WorldListener(game), parent);

    var manager = LegacyPaperCommandManager.createNative(parent, ExecutionCoordinator.simpleCoordinator());
    manager.registerBrigadier();
    BrigadierSetup.setup(manager);
    Commander.create(manager, Player.class, this).init();

    parent.getServer().getServicesManager().register(Game.class, game, parent, ServicePriority.Normal);
  }

  void onPluginDisable() {
    disable();
  }

  private void registerHooks(Server server) {
    if (server.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PlaceholderAPIHook(this).register();
    }
    if (server.getPluginManager().isPluginEnabled("MiniPlaceholders")) {
      new MiniPlaceholdersHook().init();
    }
    if (server.getPluginManager().isPluginEnabled("LuckPerms")) {
      var hook = LuckPermsHook.register(Player::getUniqueId);
      registerNamedAddon(PresetLimits.NAME, hook::presetLimits);
    }
    setupCustomCharts(new Metrics(parent, 8717));
  }

  private void setupCustomCharts(Metrics metrics) {
    metrics.addCustomChart(new SimplePie("storage_engine", () -> game.storage().toString().toLowerCase(Locale.ROOT)));
    metrics.addCustomChart(new AdvancedPie("protections", () -> Registries.PROTECTIONS.stream()
      .collect(Collectors.groupingBy(p -> p.key().value(), Collectors.summingInt(e -> 1))))
    );
    metrics.addCustomChart(new AdvancedPie("player_elements", () -> Registries.BENDERS.players()
      .map(User::elements).flatMap(Collection::stream)
      .collect(Collectors.groupingBy(Element::toString, Collectors.summingInt(e -> 1))))
    );
    metrics.addCustomChart(new SingleLineChart("bending_npc_count", Registries.BENDERS::nonPlayerCount));
  }

  @Override
  public String author() {
    return parent.getPluginMeta().getAuthors().getFirst();
  }

  @Override
  public String version() {
    return parent.getPluginMeta().getVersion();
  }
}
