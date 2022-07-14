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

package me.moros.bending.model.user.profile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds data from the database that are needed to construct the BendingProfile object
 */
public record BenderData(List<@Nullable AbilityDescription> slots, Set<Element> elements, Set<Preset> presets) {
  public static final BenderData EMPTY = new BenderData(List.of(), Set.of(), Set.of());

  public BenderData(List<@Nullable AbilityDescription> slots, Set<Element> elements, Set<Preset> presets) {
    this.slots = Collections.unmodifiableList(slots);
    this.elements = Set.copyOf(elements);
    this.presets = Set.copyOf(presets);
  }
}
