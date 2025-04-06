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

package me.moros.bending.common.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.Debounced;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.reactive.Disposable;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.util.MapFactories;

public final class ConfigManager {
  private static ConfigManager INSTANCE;

  private final Logger logger;
  private final Collection<Subscriber<? extends Configurable>> subscribers;
  private final ConfigurationReference<CommentedConfigurationNode> reference;
  private final ConfigProcessorImpl processor;
  private final Debounced<?> buffer;
  private final Disposable rootSubscriber;

  public ConfigManager(Logger logger, Path directory, WatchServiceListener listener) throws IOException {
    this.logger = logger;
    this.subscribers = new CopyOnWriteArrayList<>();
    Path path = directory.resolve("bending.conf");
    Files.createDirectories(path.getParent());
    reference = listener.listenToConfiguration(this::createLoader, path);
    reference.errors().subscribe(e -> logger.warn(e.getValue().getMessage(), e.getValue()));
    processor = new ConfigProcessorImpl(logger, reference);

    buffer = Debounced.create(this::updateSubscribers, 1, TimeUnit.SECONDS);
    rootSubscriber = reference.updates().subscribe(e -> buffer.request());

    if (INSTANCE == null) {
      INSTANCE = this;
    }
  }

  private HoconConfigurationLoader createLoader(Path path) {
    return HoconConfigurationLoader.builder().defaultOptions(withFactory()).path(path).build();
  }

  private UnaryOperator<ConfigurationOptions> withFactory() {
    return options -> options.mapFactory(MapFactories.sortedNatural())
      .serializers(builder -> builder.register(Configurable.class, ObjectMapper.factory().asTypeSerializer()));
  }

  private void updateSubscribers() {
    if (!subscribers.isEmpty()) {
      var root = config();
      subscribers.forEach(s -> s.accept(root));
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
    rootSubscriber.dispose();
    subscribers.clear();
    reference.close();
  }

  private CommentedConfigurationNode config() {
    return reference.node();
  }

  public <T extends Configurable> void subscribe(T def, Consumer<? super T> consumer) {
    Objects.requireNonNull(def);
    Objects.requireNonNull(consumer);
    var subscriber = new Subscriber<>(def, consumer);
    subscribers.add(subscriber);
    subscriber.accept(config());
  }

  public ConfigProcessor processor() {
    return processor;
  }

  public static <T extends Configurable> T load(Supplier<T> supplier) {
    return loadConfigFromNode(INSTANCE.config(), supplier.get());
  }

  public static void cache(Class<? extends Configurable> configType) {
    INSTANCE.processor.cache(configType);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Configurable> T loadConfigFromNode(ConfigurationNode node, T def) {
    try {
      return (T) node.node(def.path()).get(def.getClass(), def);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
