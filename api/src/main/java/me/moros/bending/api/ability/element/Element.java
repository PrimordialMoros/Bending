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

package me.moros.bending.api.ability.element;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import me.moros.bending.api.util.ColorPalette;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.translation.Translatable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable and thread-safe object that represents a bending element.
 */
public enum Element implements Keyed, Translatable {
  AIR("Air", ColorPalette.AIR),
  WATER("Water", ColorPalette.WATER),
  EARTH("Earth", ColorPalette.EARTH),
  FIRE("Fire", ColorPalette.FIRE);

  private final String elementName;
  private final TextColor color;
  private final Key key;

  Element(String elementName, TextColor color) {
    this.elementName = elementName;
    this.color = color;
    this.key = Key.key(NAMESPACE, elementName.toLowerCase(Locale.ROOT));
  }

  @Override
  public String toString() {
    return elementName;
  }

  @Override
  public @NonNull Key key() {
    return key;
  }

  @Override
  public @NonNull String translationKey() {
    return NAMESPACE + "." + key().value();
  }

  /**
   * Get the display name for this element.
   * @return the display name
   */
  public Component displayName() {
    return Component.translatable(translationKey(), color);
  }

  /**
   * Get the description for this element.
   * @return the description
   */
  public Component description() {
    return Component.translatable(translationKey() + ".description", color);
  }

  /**
   * Get the color for this element.
   * @return the color
   */
  public TextColor color() {
    return color;
  }

  /**
   * Get the element matching the given name.
   * @param value the element name to match
   * @return the element if found, null otherwise
   */
  public static @Nullable Element fromName(String value) {
    if (!value.isEmpty()) {
      String upper = value.toUpperCase(Locale.ROOT);
      for (Element element : VALUES) {
        if (element.name().startsWith(upper)) {
          return element;
        }
      }
    }
    return null;
  }

  public static final String NAMESPACE = "bending.element";
  public static final Collection<Element> VALUES = List.of(values());
  public static final Collection<String> NAMES = List.of("Air", "Water", "Earth", "Fire");
}
