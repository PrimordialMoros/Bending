/*
 * Copyright 2020-2022 Moros
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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;

import me.moros.bending.model.key.Key;
import me.moros.bending.model.key.Keyed;

/**
 * Represents a translation of one or more key-value pairs.
 */
public final class Translation implements Keyed, Iterable<Entry<String, MessageFormat>> {
  public static final String NAMESPACE = "bending.translation";

  private final Key key;
  private final Locale locale;
  private final Map<String, MessageFormat> formats;

  private Translation(Key key, Locale locale, Map<String, MessageFormat> formats) {
    this.key = key;
    this.locale = locale;
    this.formats = Map.copyOf(formats);
  }

  public Locale locale() {
    return locale;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Translation other) {
      return key.equals(other.key) && formats.equals(other.formats);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.key, this.formats);
  }

  @Override
  public Key key() {
    return key;
  }

  @Override
  public Iterator<Entry<String, MessageFormat>> iterator() {
    return formats.entrySet().iterator();
  }

  /**
   * Create a translation from a ResourceBundle using the default Bending Locale.
   * @param key a unique key to identify your translation when registering
   * @param bundle a bundle containing localized strings
   * @return the constructed translation
   * @throws IllegalArgumentException if bundle is empty
   */
  public static Translation fromBundle(Key key, ResourceBundle bundle) {
    return fromBundle(key, Message.DEFAULT_LOCALE, bundle);
  }

  /**
   * Create a translation from a ResourceBundle.
   * @param key a unique key to identify your translation when registering
   * @param locale a locale for the translation
   * @param bundle a bundle containing localized strings
   * @return the constructed translation
   * @throws IllegalArgumentException if bundle is empty
   */
  public static Translation fromBundle(Key key, Locale locale, ResourceBundle bundle) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(bundle);
    Map<String, MessageFormat> formats = new HashMap<>();
    for (String bundleKey : bundle.keySet()) {
      formats.put(bundleKey, new MessageFormat(bundle.getString(bundleKey), locale));
    }
    if (formats.isEmpty()) {
      throw new IllegalArgumentException("No translations found!");
    }
    return new Translation(key, locale, formats);
  }

  /**
   * Create a translation from a map of localized Strings using the default Bending Locale.
   * @param key a unique key to identify your translation when registering
   * @param translations a map with localized strings
   * @return the constructed translation
   * @throws IllegalArgumentException if translations is empty
   */
  public static Translation create(Key key, Map<String, String> translations) {
    return create(key, Message.DEFAULT_LOCALE, translations);
  }

  /**
   * Create a translation from a map of localized Strings.
   * @param key a unique key to identify your translation when registering
   * @param translations a map with localized strings
   * @return the constructed translation
   * @throws IllegalArgumentException if translations is empty
   */
  public static Translation create(Key key, Locale locale, Map<String, String> translations) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(locale);
    Objects.requireNonNull(translations);
    Map<String, MessageFormat> formats = new HashMap<>();
    for (var entry : translations.entrySet()) {
      formats.put(entry.getKey(), new MessageFormat(entry.getValue(), locale));
    }
    if (formats.isEmpty()) {
      throw new IllegalArgumentException("No translations found!");
    }
    return new Translation(key, locale, formats);
  }
}
