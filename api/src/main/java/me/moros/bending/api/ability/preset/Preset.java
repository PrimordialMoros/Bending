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

package me.moros.bending.api.ability.preset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.api.util.functional.Suppliers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable representation of slots.
 */
public final class Preset {
  public static final Preset EMPTY = from(new AbilityDescription[9]);

  private final int id;
  private final String name;
  private final AbilityDescription[] abilities;
  private final Supplier<TextColor> presetColor;

  private Preset(int id, String name, @Nullable AbilityDescription[] abilities) {
    this.id = id;
    this.name = name;
    this.abilities = new AbilityDescription[9];
    System.arraycopy(abilities, 0, this.abilities, 0, Math.min(9, abilities.length));
    this.presetColor = Suppliers.lazy(this::dominantColor);
  }

  public int id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Component displayName() {
    return Component.text(name, presetColor.get());
  }

  /**
   * Get the abilities that this preset holds.
   * @return an unmodifiable copy of the abilities
   */
  public List<@Nullable AbilityDescription> abilities() {
    return Collections.unmodifiableList(Arrays.asList(abilities));
  }

  /**
   * Check if this preset is empty.
   * @return true if this preset holds no abilities, false otherwise
   */
  public boolean isEmpty() {
    for (AbilityDescription desc : abilities) {
      if (desc != null) {
        return false;
      }
    }
    return true;
  }


  /**
   * Find the differences between this preset and another.
   * @param preset the other preset to compare against
   * @return the number of different abilities between the two presets
   */
  public int compare(Preset preset) {
    int count = 0;
    for (int slot = 0; slot < 9; slot++) {
      if (!Objects.equals(abilities[slot], preset.abilities[slot])) {
        count++;
      }
    }
    return count;
  }

  /**
   * Copy this preset to another array.
   * @param destination the array to copy to
   */
  public void copyTo(@Nullable AbilityDescription[] destination) {
    if (destination.length != 9) {
      throw new IllegalArgumentException("Destination array must be of length 9!");
    }
    System.arraycopy(abilities, 0, destination, 0, 9);
  }

  public List<Component> display() {
    List<Component> components = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      AbilityDescription desc = abilities[i];
      if (desc != null) {
        components.add(Component.text((i + 1) + ". ", ColorPalette.TEXT_COLOR).append(desc.meta()));
      }
    }
    return components;
  }

  public Component meta() {
    Component details = Component.text().append(Component.join(JoinConfiguration.newlines(), display()))
      .append(Component.newline()).append(Component.newline())
      .append(Message.HOVER_PRESET.build()).build();
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Preset other = (Preset) obj;
    return id == other.id && name.equals(other.name) && Arrays.equals(abilities, other.abilities);
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + name.hashCode();
    result = 31 * result + Arrays.hashCode(abilities);
    return result;
  }

  /**
   * Create a copy of this preset with the given id.
   * @param id the new preset id
   * @return the preset copy with the new id
   */
  public Preset withId(int id) {
    return id == this.id ? this : new Preset(id, name, abilities);
  }

  public static Preset from(AbilityDescription[] abilities) {
    return create(0, "", abilities);
  }

  /**
   * Create a new preset.
   * <br>Note: New presets must use a non-positive id as they will acquire a real one when they get saved.
   * @param id the id of the preset to create
   * @param name the name of the preset to create
   * @param abilities the abilities of the preset to create
   * @return the newly created preset
   * @throws IllegalArgumentException if preset name is invalid, use {@link TextUtil#sanitizeInput(String)} to validate.
   */
  public static Preset create(int id, String name, AbilityDescription[] abilities) {
    Objects.requireNonNull(abilities);
    String validatedName = TextUtil.sanitizeInput(name);
    if (!validatedName.equals(name)) {
      throw new IllegalArgumentException("Invalid preset name: " + name);
    }
    return new Preset(id, name, abilities);
  }
}
