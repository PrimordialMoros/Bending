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

package me.moros.bending.common;

import java.io.InputStream;
import java.nio.file.Path;

import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.ability.AbilityInitializer;
import me.moros.bending.common.config.BendingPropertiesImpl;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.game.GameImpl;
import me.moros.bending.common.loader.AddonLoader;
import me.moros.bending.common.locale.TranslationManager;
import me.moros.bending.common.util.GameProviderUtil;
import me.moros.bending.common.util.ReflectionUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public abstract class AbstractBending<T> implements Bending {
  protected final T parent;

  private final Path path;
  private final Logger logger;

  private final AddonLoader addonLoader;
  private final ConfigManager configManager;
  private final TranslationManager translationManager;

  protected Game game;

  protected AbstractBending(T parent, Path dir, Logger logger) {
    this.parent = parent;
    this.path = dir;
    this.logger = logger;
    this.addonLoader = AddonLoader.create(dir, getClass().getClassLoader());
    this.configManager = new ConfigManager(logger, dir);
    this.translationManager = new TranslationManager(logger, dir);
    ReflectionUtil.injectStatic(BendingProperties.Holder.class, ConfigManager.load(BendingPropertiesImpl::new));
    new AbilityInitializer().init();
    addonLoader.loadAll();
  }

  protected void load() {
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

  protected void disable() {
    if (game != null) {
      addonLoader.unloadAll();
      game.cleanup();
      EventBus.INSTANCE.shutdown();
      configManager().close();
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
