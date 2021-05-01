/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.Bending;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

public class TranslationManager {
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
  private final Path translationsDirectory;
  private TranslationRegistry registry;

  public TranslationManager(@NonNull String directory) {
    translationsDirectory = Paths.get(directory, "translations");
    reload();
  }

  public void reload() {
    if (registry != null) {
      GlobalTranslator.get().removeSource(registry);
      installed.clear();
    }
    registry = TranslationRegistry.create(Key.key("bending", "translations"));
    registry.defaultLocale(DEFAULT_LOCALE);

    loadCustom();

    ResourceBundle bundle = ResourceBundle.getBundle("bending", DEFAULT_LOCALE, UTF8ResourceBundleControl.get());
    registry.registerAll(DEFAULT_LOCALE, bundle, false);
    GlobalTranslator.get().addSource(registry);
  }

  private void loadCustom() {
    Collection<Path> files;
    try (Stream<Path> stream = Files.list(translationsDirectory)) {
      files = stream.filter(this::isValidPropertyFile).collect(Collectors.toList());
    } catch (IOException e) {
      files = Collections.emptyList();
    }
    files.forEach(this::loadTranslationFile);
    int amount = installed.size();
    if (amount > 0) {
      String translations = installed.stream().map(Locale::getLanguage).collect(Collectors.joining(", ", "[", "]"));
      Bending.logger().info("Loaded " + amount + " translations: " + translations);
    }
  }

  private void loadTranslationFile(Path path) {
    String localeString = removeFileExtension(path);
    Locale locale = Translator.parseLocale(localeString);
    if (locale == null) {
      Bending.logger().warn("Unknown locale: " + localeString);
      return;
    }
    PropertyResourceBundle bundle;
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      bundle = new PropertyResourceBundle(reader);
    } catch (IOException e) {
      Bending.logger().warn("Error loading locale file: " + localeString);
      return;
    }
    registry.registerAll(locale, bundle, false);
    installed.add(locale);
  }

  private boolean isValidPropertyFile(Path path) {
    if (!Files.isRegularFile(path)) {
      return false;
    }
    String name = path.getFileName().toString();
    return name.length() > "properties".length() && name.endsWith(".properties");
  }

  private String removeFileExtension(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.substring(0, fileName.length() - ".properties".length());
  }

  public @Nullable TranslatableComponent getTranslation(@NonNull String key) {
    return registry.contains(key) ? Component.translatable(key) : null;
  }
}
