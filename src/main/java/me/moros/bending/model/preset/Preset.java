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
import java.util.Objects;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * An immutable representation of slots.
 */
public final class Preset {
  public static final Preset EMPTY = new Preset(new String[9]);

  private final int id;
  private final String name;
  private final String[] abilities;

  /**
   * Presets loaded from db have a positive id.
   * New presets must use a non positive id as they will acquire a real one when they get saved.
   */
  public Preset(int id, @NonNull String name, @NonNull String[] abilities) {
    this.id = id;
    this.name = name;
    this.abilities = abilities;
  }

  /**
   * Creates a dummy preset with id 0 and an empty name.
   * @see #Preset(int, String, String[])
   */
  public Preset(@NonNull String[] abilities) {
    this(0, "", abilities);
  }

  public int id() {
    return id;
  }

  public @NonNull String name() {
    return name;
  }

  /**
   * Returns an array of the ability names that this preset holds, names can be null!
   * @return a copy of the names of the abilities that this preset holds
   */
  public @NonNull String[] abilities() {
    return Arrays.copyOf(abilities, 9);
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
    String[] otherAbilities = preset.abilities();
    int count = 0;
    for (int slot = 0; slot < 9; slot++) {
      if (!Objects.equals(abilities[slot], otherAbilities[slot])) {
        count++;
      }
    }
    return count;
  }

  public @NonNull AbilityDescription[] toBinds() {
    AbilityDescription[] mapped = new AbilityDescription[9];
    for (int slot = 0; slot < 9; slot++) {
      mapped[slot] = Bending.game().abilityRegistry().abilityDescription(abilities[slot]).orElse(null);
    }
    return mapped;
  }
}
