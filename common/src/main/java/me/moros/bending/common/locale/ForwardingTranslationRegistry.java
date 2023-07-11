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

package me.moros.bending.common.locale;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.TranslationRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

record ForwardingTranslationRegistry(TranslationRegistry handle, Set<Locale> locales) implements TranslationRegistry {
  ForwardingTranslationRegistry(Key name) {
    this(TranslationRegistry.create(name), ConcurrentHashMap.newKeySet());
  }

  @Override
  public @NonNull Key name() {
    return handle().name();
  }

  @Override
  public boolean contains(@NonNull String key) {
    return handle().contains(key);
  }

  @Override
  public @Nullable MessageFormat translate(@NonNull String key, @NonNull Locale locale) {
    return handle().translate(key, locale);
  }

  @Override
  public @Nullable Component translate(@NonNull TranslatableComponent component, @NonNull Locale locale) {
    return handle().translate(component, locale);
  }

  @Override
  public void defaultLocale(@NonNull Locale locale) {
    handle().defaultLocale(locale);
  }

  @Override
  public void register(@NonNull String key, @NonNull Locale locale, @NonNull MessageFormat format) {
    handle().register(key, locale, format);
    locales().add(locale);
  }

  @Override
  public void unregister(@NonNull String key) {
    handle().unregister(key);
  }
}
