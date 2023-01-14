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

package me.moros.bending.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import me.moros.bending.model.registry.Registries;
import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

/**
 * TranslationManager loads localized strings and adds them to a {@link TranslationRegistry} that can be used
 * to create {@link TranslatableComponent}.
 * @see Message
 */
public final class TranslationManager implements Iterable<Locale> {
  private final Logger logger;
  private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
  private final Path translationsDirectory;
  private TranslationRegistry registry;

  public TranslationManager(Logger logger, Path directory) {
    this.logger = logger;
    translationsDirectory = directory.resolve("translations");
    reload();
  }

  public void reload() {
    if (registry != null) {
      GlobalTranslator.translator().removeSource(registry);
      installed.clear();
    }
    registry = TranslationRegistry.create(KeyUtil.simple("translations"));
    registry.defaultLocale(Message.DEFAULT_LOCALE);

    loadCustom();

    ResourceBundle bundle = ResourceBundle.getBundle("bending", Message.DEFAULT_LOCALE, UTF8ResourceBundleControl.get());
    registry.registerAll(Message.DEFAULT_LOCALE, bundle, false);

    loadFromRegistry();

    installed.add(Message.DEFAULT_LOCALE);
    GlobalTranslator.translator().addSource(registry);
  }

  private void loadFromRegistry() {
    for (Translation translation : Registries.TRANSLATIONS) {
      Locale locale = translation.locale();
      for (var entry : translation) {
        String key = entry.getKey();
        if (!registry.contains(key)) {
          registry.register(key, locale, entry.getValue());
        }
      }
      installed.add(locale);
    }
  }

  private void loadCustom() {
    Collection<Path> files;
    try (Stream<Path> stream = Files.list(translationsDirectory)) {
      files = stream.filter(this::isValidPropertyFile).toList();
    } catch (IOException e) {
      files = List.of();
    }
    files.forEach(this::loadTranslationFile);
  }

  private void loadTranslationFile(Path path) {
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
    registry.registerAll(locale, bundle, false);
    installed.add(locale);
  }

  private boolean isValidPropertyFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".properties");
  }

  private String removeFileExtension(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.substring(0, fileName.length() - ".properties".length());
  }

  /**
   * Attempt to retrieve the translation for the specified key.
   * @param key a translation key
   * @return the translatable component for the given key or null if key is not registered
   */
  public @Nullable TranslatableComponent translate(String key) {
    return registry.contains(key) ? Component.translatable(key) : null;
  }

  @Override
  public Iterator<Locale> iterator() {
    return Collections.unmodifiableSet(installed).iterator();
  }
}
