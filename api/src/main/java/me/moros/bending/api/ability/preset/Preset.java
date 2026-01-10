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

package me.moros.bending.api.ability.preset;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.api.util.functional.Suppliers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jspecify.annotations.Nullable;

/**
 * An immutable representation of slots.
 */
public final class Preset {
  private static final Preset EMPTY = new Preset("", new AbilityDescription[9]);

  private final String name;
  private final AbilityDescription[] abilities;
  private final Supplier<TextColor> presetColor;

  private Preset(String name, Preset other) {
    this.name = name;
    this.abilities = other.abilities;
    this.presetColor = other.presetColor;
  }

  private Preset(String name, @Nullable AbilityDescription[] abilities) {
    this.name = name;
    this.abilities = new AbilityDescription[9];
    System.arraycopy(abilities, 0, this.abilities, 0, Math.min(9, abilities.length));
    this.presetColor = Suppliers.lazy(this::dominantColor);
  }

  public String name() {
    return name;
  }

  public Component displayName() {
    return Component.text(name, presetColor.get());
  }

  /**
   * Get the abilities that this preset holds as a list.
   * @return an immutable list with the abilities
   */
  public List<@Nullable AbilityDescription> abilities() {
    return Collections.unmodifiableList(Arrays.asList(abilities));
  }

  /**
   * Check if this preset is empty.
   * @return true if this preset holds no abilities, false otherwise
   */
  public boolean isEmpty() {
    return matchesBinds(EMPTY);
  }

  /**
   * Check if this preset matches the binds of another.
   * @param other the other preset to compare against
   * @return true if the binds match, false otherwise
   */
  public boolean matchesBinds(Preset other) {
    return matchesBinds(other.abilities);
  }

  /**
   * Check if this preset matches the given binds.
   * @param other the other binds to compare against
   * @return true if the binds match, false otherwise
   */
  public boolean matchesBinds(AbilityDescription[] other) {
    return Arrays.equals(abilities, other);
  }

  /**
   * Copy this preset to another array.
   * @param destination the array to copy to
   */
  public void copyTo(AbilityDescription[] destination) {
    System.arraycopy(abilities, 0, destination, 0, 9);
  }

  public void forEach(ObjIntConsumer<AbilityDescription> consumer) {
    for (int idx = 0; idx < abilities.length; idx++) {
      AbilityDescription desc = abilities[idx];
      if (desc != null) {
        consumer.accept(desc, idx);
      }
    }
  }

  private TextColor dominantColor() {
    Map<Element, Integer> counter = new EnumMap<>(Element.class);
    forEach((desc, idx) -> desc.elements().forEach(element -> counter.merge(element, 1, Integer::sum)));
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
    return name.equals(other.name) && Arrays.equals(abilities, other.abilities);
  }

  @Override
  public int hashCode() {
    return 31 * name.hashCode() + Arrays.hashCode(abilities);
  }

  public Preset withName(String newName) {
    if (isEmpty() || this.name.equals(newName)) {
      return this;
    }
    validateName(newName);
    return new Preset(newName, this);
  }

  public static Preset from(AbilityDescription[] abilities) {
    Objects.requireNonNull(abilities);
    return createOrReuseEmpty("", abilities);
  }

  /**
   * Create a new preset.
   * @param name the name of the preset to create
   * @param abilities the abilities of the preset to create
   * @return the newly created preset
   * @throws IllegalArgumentException if preset name is invalid, use {@link TextUtil#sanitizeInput(String)} to validate
   */
  public static Preset create(String name, AbilityDescription[] abilities) {
    Objects.requireNonNull(abilities);
    validateName(name);
    return createOrReuseEmpty(name, abilities);
  }

  private static Preset createOrReuseEmpty(String name, AbilityDescription[] abilities) {
    if (EMPTY.matchesBinds(abilities)) {
      return EMPTY;
    }
    return new Preset(name, abilities);
  }

  public static Preset empty() {
    return EMPTY;
  }

  private static void validateName(String name) throws IllegalArgumentException {
    String validatedName = TextUtil.sanitizeInput(name);
    if (!validatedName.equals(name)) {
      throw new IllegalArgumentException("Invalid preset name: " + name);
    }
  }
}
