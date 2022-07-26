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

package me.moros.bending.model;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import me.moros.bending.model.key.Key;
import me.moros.bending.model.key.Keyed;
import me.moros.bending.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable and thread-safe object that represents a bending element
 */
public enum Element implements Keyed {
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
    this.key = Key.create(NAMESPACE, elementName.toLowerCase(Locale.ROOT));
  }

  @Override
  public String toString() {
    return elementName;
  }

  public Key key() {
    return key;
  }

  public Component displayName() {
    return Component.translatable(key().toString(), color);
  }

  public Component description() {
    return Component.translatable(key() + ".description", color);
  }

  public TextColor color() {
    return color;
  }

  public static final String NAMESPACE = "bending.element";

  public static Optional<Element> fromName(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }
    String upper = value.toUpperCase(Locale.ROOT);
    return VALUES.stream().filter(e -> e.name().startsWith(upper)).findAny();
  }

  public static final Collection<Element> VALUES = List.of(values());

  public static final Collection<String> NAMES = List.of("Air", "Water", "Earth", "Fire");
}
