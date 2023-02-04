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

package me.moros.bending.api.user.profile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents bender data.
 */
public interface BenderProfile {
  /**
   * The bender's bound abilities.
   * @return an immutable list of the bender's bound abilities
   */
  List<@Nullable AbilityDescription> slots();

  /**
   * The bender's elements.
   * @return an immutable set of the bender's elements
   */
  Set<Element> elements();

  /**
   * The bender's presets.
   * @return an immutable list of the bender's bound abilities
   */
  Set<Preset> presets();

  static BenderProfile empty() {
    return BenderProfileImpl.EMPTY;
  }

  static BenderProfile of(List<@Nullable AbilityDescription> slots, Set<Element> elements, Set<Preset> presets) {
    return new BenderProfileImpl(Collections.unmodifiableList(slots), Set.copyOf(elements), Set.copyOf(presets));
  }

  static PlayerBenderProfile of(int id, boolean board) {
    return of(id, board, empty());
  }

  static PlayerBenderProfile of(int id, boolean board, BenderProfile benderProfile) {
    return new PlayerBenderProfileImpl(id, board, benderProfile);
  }
}
