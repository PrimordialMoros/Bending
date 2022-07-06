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

package me.moros.bending.model.preset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable representation of slots.
 */
public final class Preset {
  public static final Preset EMPTY = new Preset(new AbilityDescription[9]);

  private final int id;
  private final String name;
  private final AbilityDescription[] abilities;
  private TextColor presetColor;

  /**
   * Presets loaded from db have a positive id.
   * New presets must use a non positive id as they will acquire a real one when they get saved.
   */
  public Preset(int id, @NonNull String name, @Nullable AbilityDescription @NonNull [] abilities) {
    this.id = id;
    this.name = name;
    this.abilities = new AbilityDescription[9];
    System.arraycopy(abilities, 0, this.abilities, 0, Math.min(9, abilities.length));
  }

  /**
   * Creates a dummy preset with id 0 and an empty name.
   * @see #Preset(int, String, AbilityDescription[])
   */
  public Preset(@Nullable AbilityDescription @NonNull [] abilities) {
    this(0, "", abilities);
  }

  public int id() {
    return id;
  }

  public @NonNull String name() {
    return name;
  }

  public @NonNull Component displayName() {
    if (presetColor == null) {
      presetColor = dominantColor();
    }
    return Component.text(name, presetColor);
  }

  /**
   * @return a copy of the names of the abilities that this preset holds
   */
  public @NonNull List<@Nullable AbilityDescription> abilities() {
    return Arrays.asList(abilities);
  }

  public boolean isEmpty() {
    for (AbilityDescription desc : abilities) {
      if (desc != null) {
        return false;
      }
    }
    return true;
  }

  public int compare(@NonNull Preset preset) {
    int count = 0;
    for (int slot = 0; slot < 9; slot++) {
      if (!Objects.equals(abilities[slot], preset.abilities[slot])) {
        count++;
      }
    }
    return count;
  }

  public void copyTo(@Nullable AbilityDescription @NonNull [] destination) {
    if (destination.length != 9) {
      throw new IllegalArgumentException("Destination array must be of length 9!");
    }
    System.arraycopy(abilities, 0, destination, 0, 9);
  }

  public @NonNull List<@NonNull Component> display() {
    List<Component> components = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      AbilityDescription desc = abilities[i];
      if (desc != null) {
        components.add(Component.text((i + 1) + ". ", ColorPalette.TEXT_COLOR).append(desc.meta()));
      }
    }
    return components;
  }

  public @NonNull Component meta() {
    Component details = Component.text().append(Component.join(JoinConfiguration.newlines(), display()))
      .append(Component.newline()).append(Component.newline())
      .append(Component.text("Click to bind this preset.", ColorPalette.NEUTRAL)).build();

    return displayName()
      .hoverEvent(HoverEvent.showText(details))
      .clickEvent(ClickEvent.runCommand("/bending preset bind " + name()));
  }

  private TextColor dominantColor() {
    Map<Element, Integer> counter = new EnumMap<>(Element.class);
    for (AbilityDescription desc : abilities) {
      if (desc != null) {
        counter.compute(desc.element(), (k, v) -> (v == null) ? 1 : v + 1);
      }
    }
    return counter.entrySet().stream().max(Entry.comparingByValue())
      .map(e -> e.getKey().color()).orElse(ColorPalette.NEUTRAL);
  }
}
