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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.common.logging.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.WatchServiceListener;

public final class ConfigManager implements Closeable {
  private static ConfigManager INSTANCE;

  private final Logger logger;
  private final ConfigurationReference<CommentedConfigurationNode> reference;
  private final ConfigProcessorImpl processor;

  public ConfigManager(Logger logger, Path directory, WatchServiceListener listener) throws IOException {
    this.logger = logger;
    Path path = directory.resolve("bending.conf");
    Files.createDirectories(path.getParent());
    reference = listener.listenToConfiguration(f -> HoconConfigurationLoader.builder().path(f).build(), path);
    processor = new ConfigProcessorImpl(logger, reference);
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

  @Override
  public void close() {
    reference.close();
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
