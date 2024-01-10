/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.ability.AbilityInitializer;
import me.moros.bending.common.config.BendingPropertiesImpl;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.game.GameImpl;
import me.moros.bending.common.loader.AddonLoader;
import me.moros.bending.common.locale.TranslationManager;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.GameProviderUtil;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.tasker.executor.SyncExecutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.reference.WatchServiceListener;

public abstract class AbstractBending<T> implements Bending {
  protected final T parent;

  private final Path path;
  private final Logger logger;

  private final WatchServiceListener listener;
  private final ConfigManager configManager;
  private final TranslationManager translationManager;
  private final AddonLoader addonLoader;

  protected Game game;

  protected AbstractBending(T parent, Path dir, Logger logger) {
    this.parent = parent;
    this.path = dir;
    this.logger = logger;
    try {
      this.listener = WatchServiceListener.create();
      this.configManager = new ConfigManager(logger, dir, listener);
      this.translationManager = new TranslationManager(logger, dir, listener);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    this.addonLoader = AddonLoader.create(logger(), path, getClass().getClassLoader());
    this.configManager.subscribe(new BendingPropertiesImpl(), this::injectProperties);
    new AbilityInitializer().init();
  }

  protected void injectTasker(SyncExecutor syncExecutor) {
    ReflectionUtil.injectStatic(Tasker.class, syncExecutor);
  }

  private void injectProperties(BendingProperties properties) {
    ReflectionUtil.injectStatic(BendingProperties.Holder.class, properties);
  }

  protected Collection<Supplier<Addon>> addonProviders() {
    return Set.of();
  }

  protected void load() {
    addonLoader.loadAll(addonProviders());
    translationManager.refresh(); // Refresh to load addon translations
    game = new GameImpl(this);
    GameProviderUtil.registerProvider(game);
    addonLoader.enableAll(game);
  }

  @Override
  public void reload() {
    if (game != null) {
      game.reload();
    }
  }

  protected void softDisable() {
    if (game != null) {
      addonLoader.unloadAll();
      game.cleanup();
      game.storage().close();
      Tasker.sync().clear(); // Clear any sync tasks
      GameProviderUtil.unregisterProvider();
      game = null;
    }
  }

  protected void disable() {
    if (game != null) {
      addonLoader.unloadAll();
      game.cleanup();
      game.eventBus().shutdown();
      configManager().close();
      try {
        listener.close();
      } catch (IOException e) {
        logger.warn(e.getMessage(), e);
      }
      Tasker.sync().shutdown();
      Tasker.async().shutdown();
      game.storage().close();
      GameProviderUtil.unregisterProvider();
      game = null;
    }
  }

  @Override
  public Path path() {
    return path;
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
}
