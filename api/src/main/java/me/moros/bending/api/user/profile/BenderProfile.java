/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.user.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.util.collect.ElementSet;

/**
 * Represents bender data.
 */
public sealed interface BenderProfile permits BenderProfileImpl {
  UUID uuid();

  boolean board();

  /**
   * The bender's bound abilities.
   * @return the bender's bound abilities as a dummy preset
   */
  Preset slots();

  /**
   * The bender's elements.
   * @return an immutable collection of the bender's elements
   */
  ElementSet elements();

  /**
   * The bender's presets.
   * @return an immutable collection of the bender's presets
   */
  Map<String, Preset> presets();

  static BenderProfile of(UUID uuid) {
    return new BenderProfileImpl(uuid, true, ElementSet.of(), Preset.empty(), Map.of());
  }

  static BenderProfile of(UUID uuid, Collection<Element> elements, Preset slots, Collection<Preset> presets) {
    return of(uuid, true, elements, slots, presets);
  }

  static BenderProfile of(UUID uuid, boolean board, Collection<Element> elements, Preset slots, Collection<Preset> presets) {
    Objects.requireNonNull(uuid);
    ElementSet elementsCopy = ElementSet.copyOf(elements);
    // Ensure unique, valid names for presets in stable order
    Map<String, Preset> map = new LinkedHashMap<>();
    presets.stream()
      .filter(p -> !p.name().isEmpty() && !p.isEmpty())
      .sorted(Comparator.comparing(Preset::name))
      .forEach(p -> map.put(p.name(), p));
    return new BenderProfileImpl(uuid, board, elementsCopy, slots.withName(""), Collections.unmodifiableMap(map));
  }
}
