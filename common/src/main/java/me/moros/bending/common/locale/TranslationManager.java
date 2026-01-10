/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.Debounced;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.translation.Translator;
import org.spongepowered.configurate.reference.WatchServiceListener;

/**
 * TranslationManager loads localized strings and adds them to a {@link TranslationStore} that can be used
 * to create {@link TranslatableComponent}.
 * @see Message
 */
public final class TranslationManager implements Iterable<Locale> {
  private static final String PATH = "bending.lang.messages_en";

  private final Logger logger;
  private final Path translationsDirectory;
  private final Set<Locale> localeSet;
  private final AtomicReference<TranslationStore.StringBased<MessageFormat>> registryReference;
  private final Debounced<?> buffer;

  public TranslationManager(Logger logger, Path directory, WatchServiceListener listener) throws IOException {
    this.logger = logger;
    this.translationsDirectory = Files.createDirectories(directory.resolve("translations"));
    this.localeSet = new CopyOnWriteArraySet<>();
    this.registryReference = new AtomicReference<>(createRegistry());
    GlobalTranslator.translator().addSource(registryReference.get());
    this.buffer = Debounced.create(this::reload, 2, TimeUnit.SECONDS);
    listener.listenToDirectory(translationsDirectory, e -> refresh());
  }

  public void refresh() {
    buffer.request();
  }

  private void reload() {
    var newRegistry = createRegistry();
    var old = registryReference.getAndSet(newRegistry);
    GlobalTranslator.translator().removeSource(old);
    GlobalTranslator.translator().addSource(newRegistry);
    logger.info("Registered translations: " + TextUtil.collect(localeSet, Locale::getLanguage));
  }

  private TranslationStore.StringBased<MessageFormat> createRegistry() {
    localeSet.clear();
    var registry = TranslationStore.messageFormat(KeyUtil.simple("translations"));
    registry.defaultLocale(Translation.DEFAULT_LOCALE);
    loadCustom(registry);
    loadDefaults(registry);
    loadFromRegistry(registry);
    return registry;
  }

  private void loadDefaults(TranslationStore.StringBased<MessageFormat> registry) {
    ResourceBundle bundle = ResourceBundle.getBundle(PATH, Translation.DEFAULT_LOCALE);
    registerBundle(registry, Translation.DEFAULT_LOCALE, bundle);
  }

  private void loadCustom(TranslationStore.StringBased<MessageFormat> registry) {
    Collection<Path> paths;
    try (Stream<Path> stream = Files.list(translationsDirectory)) {
      paths = stream.filter(TranslationManager::isValidPropertyFile).toList();
    } catch (IOException e) {
      paths = List.of();
    }
    paths.forEach(path -> loadTranslationFile(path, registry));
  }

  private void loadTranslationFile(Path path, TranslationStore.StringBased<MessageFormat> registry) {
    String localeString = removeFileExtension(path);
    Locale locale = Translator.parseLocale(localeString);
    if (locale == null) {
      logger.warn("Unknown locale: " + localeString);
      return;
    }
    PropertyResourceBundle bundle;
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      bundle = new PropertyResourceBundle(reader);
    } catch (IOException e) {
      logger.warn("Error loading locale file: " + localeString);
      return;
    }
    registerBundle(registry, locale, bundle);
  }

  private void registerBundle(TranslationStore.StringBased<MessageFormat> registry, Locale locale, ResourceBundle bundle) {
    registry.registerAll(locale, bundle, false);
    localeSet.add(locale);
  }

  private void loadFromRegistry(TranslationStore.StringBased<MessageFormat> registry) {
    for (Translation translation : Registries.TRANSLATIONS) {
      Locale locale = translation.locale();
      localeSet.add(locale);
      for (var entry : translation) {
        String key = entry.getKey();
        if (!registry.contains(key)) {
          registry.register(key, locale, entry.getValue());
        }
      }
    }
  }

  private static boolean isValidPropertyFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".properties");
  }

  private static String removeFileExtension(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.substring(0, fileName.length() - ".properties".length());
  }

  @Override
  public Iterator<Locale> iterator() {
    return localeSet.iterator();
  }
}
