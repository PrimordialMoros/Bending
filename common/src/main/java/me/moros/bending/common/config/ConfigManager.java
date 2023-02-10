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

package me.moros.bending.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.WatchServiceListener;

public final class ConfigManager {
  private static ConfigManager INSTANCE;

  private final Logger logger;
  private final WatchServiceListener listener;
  private final ConfigurationReference<CommentedConfigurationNode> reference;
  private final ConfigProcessorImpl processor;

  public ConfigManager(Logger logger, Path directory) {
    this.logger = logger;
    Path path = directory.resolve("bending.conf");
    try {
      Files.createDirectories(path.getParent());
      listener = WatchServiceListener.create();
      reference = listener.listenToConfiguration(f -> HoconConfigurationLoader.builder().path(f).build(), path);
      processor = new ConfigProcessorImpl(logger, reference);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (INSTANCE == null) {
      INSTANCE = this;
    }
  }

  public void save() {
    try {
      reference.save();
    } catch (IOException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public void close() {
    try {
      reference.close();
      listener.close();
    } catch (IOException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public CommentedConfigurationNode config() {
    return reference.node();
  }

  public ConfigProcessor processor() {
    return processor;
  }

  public static <T extends Configurable> T load(Supplier<T> supplier) {
    return INSTANCE.processor.get(supplier.get());
  }
}
