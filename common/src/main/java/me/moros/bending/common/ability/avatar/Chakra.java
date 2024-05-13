/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.ability.avatar;

import java.util.List;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum Chakra {
  AIR_CHAKRA(Element.AIR),
  WATER_CHAKRA(Element.WATER),
  EARTH_CHAKRA(Element.EARTH),
  FIRE_CHAKRA(Element.FIRE),
  SOUND_CHAKRA(Component.text("Sound", NamedTextColor.DARK_GRAY), null),
  LIGHT_CHAKRA(Component.text("Light", NamedTextColor.WHITE), null),
  THOUGHT_CHAKRA(Component.text("Thought", ColorPalette.AVATAR), null);

  private final Component name;
  private final @Nullable Element element;

  Chakra(Element element) {
    this(element.displayName(), element);
  }

  Chakra(Component name, @Nullable Element element) {
    this.name = name;
    this.element = element;
  }

  public Component displayName() {
    return name;
  }

  public @Nullable Element element() {
    return element;
  }

  public static final List<Chakra> VALUES = List.of(values());

  public static Chakra elementalChakra(Element element) {
    return switch (element) {
      case AIR -> AIR_CHAKRA;
      case WATER -> WATER_CHAKRA;
      case EARTH -> EARTH_CHAKRA;
      case FIRE -> FIRE_CHAKRA;
    };
  }
}
