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

package me.moros.bending.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.serialize.SerializationException;

public final class ConfigManager {
  private static ConfigManager INSTANCE;

  private final Logger logger;
  private final WatchServiceListener listener;
  private final ConfigurationReference<CommentedConfigurationNode> reference;
  private final ConfigProcessor processor;

  public ConfigManager(Logger logger, Path directory) {
    this.logger = logger;
    Path path = directory.resolve("bending.conf");
    try {
      Files.createDirectories(path.getParent());
      listener = WatchServiceListener.create();
      reference = listener.listenToConfiguration(f -> HoconConfigurationLoader.builder().path(f).build(), path);
      processor = new ConfigProcessor(logger, reference);
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

  public <T extends Configurable> @Nullable ValueReference<T, CommentedConfigurationNode> reference(Class<T> clazz, T config) {
    NodePath path = NodePath.of(List.of(config.path()));
    try {
      return reference.referenceTo(clazz, path, config);
    } catch (SerializationException e) {
      logger.error(e.getMessage(), e);
      return null;
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
