/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.preset;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable representation of slots.
 */
public final class Preset {
  public static final Preset EMPTY = new Preset(List.of());

  private final int id;
  private final String name;
  private final String[] abilities;

  /**
   * Presets loaded from db have a positive id.
   * New presets must use a non positive id as they will acquire a real one when they get saved.
   */
  public Preset(int id, @NonNull String name, @NonNull List<@Nullable String> abilities) {
    this.id = id;
    this.name = name;
    this.abilities = new String[9];
    for (int slot = 0; slot < Math.min(9, abilities.size()); slot++) {
      this.abilities[slot] = abilities.get(slot);
    }
  }

  /**
   * Creates a dummy preset with id 0 and an empty name.
   * @see #Preset(int, String, List)
   */
  public Preset(@NonNull List<@Nullable String> abilities) {
    this(0, "", abilities);
  }

  public int id() {
    return id;
  }

  public @NonNull String name() {
    return name;
  }

  /**
   * @return an immutable copy of the names of the abilities that this preset holds
   */
  public @NonNull List<@Nullable String> abilities() {
    return List.of(abilities);
  }

  public boolean isEmpty() {
    for (String s : abilities) {
      if (s != null) {
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

  public @NonNull List<@Nullable AbilityDescription> toBinds() {
    return Arrays.stream(abilities).map(Registries.ABILITIES::ability).collect(Collectors.toList());
  }
}
