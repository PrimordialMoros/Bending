/*
 * Copyright 2020-2022 Moros
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

import java.util.Locale;
import java.util.Objects;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.command.CommandManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.game.GameImpl;
import me.moros.bending.hook.LuckPermsHook;
import me.moros.bending.hook.placeholder.BendingExpansion;
import me.moros.bending.listener.BlockListener;
import me.moros.bending.listener.EntityListener;
import me.moros.bending.listener.PlayerListener;
import me.moros.bending.locale.TranslationManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.protection.WorldGuardFlag;
import me.moros.bending.registry.Registries;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.TextUtil;
import me.moros.bending.util.VersionUtil;
import me.moros.bending.util.metadata.Metadata;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class Bending extends JavaPlugin {
  private Logger logger;
  private String author;
  private String version;

  private ConfigManager configManager;
  private TranslationManager translationManager;

  private BendingStorage storage;
  private Game game;

  @Override
  public void onEnable() {
    new Metrics(this, 8717);
    loadAdapter();
    Metadata.inject(this);
    Tasker.INSTANCE.inject(this);

    new ProtectionInitializer(getServer().getPluginManager(), configManager);
    BendingProperties.inject(ConfigManager.load(BendingPropertiesImpl::new));
    game = new GameImpl(this, configManager, storage);

    printInfo();

    getServer().getPluginManager().registerEvents(new BlockListener(game), this);
    getServer().getPluginManager().registerEvents(new EntityListener(game), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this, game), this);

    try {
      new CommandManager(this, game);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    configManager.save();
    getServer().getServicesManager().register(Game.class, game, this, ServicePriority.Normal);
    registerHooks();
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.cleanup(true);
    }
    configManager.close();
  }

  @Override
  public void onLoad() {
    logger = getSLF4JLogger();
    author = getDescription().getAuthors().get(0);
    version = getDescription().getVersion();

    String dir = getDataFolder().toString();
    configManager = new ConfigManager(logger, dir);
    translationManager = new TranslationManager(logger, dir);

    storage = StorageFactory.createInstance(logger, dir);
    Objects.requireNonNull(storage, "Unable to establish database connection!").init(this);
    new AbilityInitializer();

    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
      WorldGuardFlag.registerFlag();
    }
  }

  private void printInfo() {
    int abilityAmount = Registries.ABILITIES.size();
    int sequenceAmount = Registries.SEQUENCES.size();
    int collisionAmount = Registries.COLLISIONS.size();
    logger.info(String.format("Found %d registered abilities (%d Sequences)!", abilityAmount, sequenceAmount));
    logger.info(String.format("Found %d registered collisions!", collisionAmount));
    logger.info("Registered protection plugins: " + TextUtil.collect(Registries.PROTECTIONS));
    logger.info("Registered translations: " + TextUtil.collect(translationManager, Locale::getLanguage));
  }

  private @Nullable NativeAdapter findAdapter(String className) {
    try {
      Class<?> cls = Class.forName(className);
      if (!cls.isSynthetic() && NativeAdapter.class.isAssignableFrom(cls)) {
        return (NativeAdapter) cls.getDeclaredConstructor().newInstance();
      }
    } catch (Exception ignore) {
    }
    return null;
  }

  private void loadAdapter() {
    String className = "me.moros.bending.adapter.impl." + VersionUtil.nmsVersion() + ".NativeAdapterImpl";
    NativeAdapter adapter = findAdapter(className);
    if (adapter != null) {
      NativeAdapter.inject(adapter);
    }
    if (NativeAdapter.hasNativeSupport()) {
      logger.info("Successfully loaded native adapter for version " + VersionUtil.nmsVersion());
    } else {
      String s = String.format("""
                
        ****************************************************************
        * Unable to find native adapter for version %s.
        * Some features may be unsupported (for example toast notifications) or induce significant overhead.
        * Packet based abilities will utilize real entities instead which can be slower when spawned in large amounts.
        * It is recommended you find a supported version.
        ****************************************************************
                
        """, VersionUtil.nmsVersion());
      logger.warn(s);
    }
  }

  private void registerHooks() {
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new BendingExpansion(this).register();
    }
    if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
      new LuckPermsHook(getServer().getServicesManager());
    }
  }

  public String author() {
    return author;
  }

  public String version() {
    return version;
  }

  public Logger logger() {
    return logger;
  }

  public TranslationManager translationManager() {
    return translationManager;
  }
}
