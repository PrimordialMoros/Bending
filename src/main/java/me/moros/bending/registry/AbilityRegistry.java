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

package me.moros.bending.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import me.moros.bending.model.ability.description.AbilityDescription;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds all the registered AbilityDescriptions for the current session.
 */
public final class AbilityRegistry implements Registry<AbilityDescription> {
  private final Map<String, AbilityDescription> abilities;

  AbilityRegistry() {
    abilities = new ConcurrentHashMap<>();
  }

  /**
   * Registers a collection of AbilityDescriptions.
   * @param abilities the abilities to register
   * @return the number of newly registered abilities
   */
  public int register(@NonNull Collection<@NonNull AbilityDescription> abilities) {
    int counter = 0;
    for (AbilityDescription desc : abilities) {
      if (register(desc)) {
        counter++;
      }
    }
    return counter;
  }

  /**
   * Registers an AbilityDescription if it doesn't already exist.
   * @param desc the ability to register
   * @return true if a new AbilityDescription was registered, false otherwise
   */
  public boolean register(@NonNull AbilityDescription desc) {
    if (!contains(desc)) {
      abilities.put(desc.name().toLowerCase(), desc);
      return true;
    }
    return false;
  }

  /**
   * @param name the name to match
   * @return the ability description or null if not found
   */
  public @Nullable AbilityDescription ability(@Nullable String name) {
    return (name == null || name.isEmpty()) ? null : abilities.get(name.toLowerCase());
  }

  public boolean contains(@NonNull AbilityDescription desc) {
    return abilities.containsKey(desc.name().toLowerCase());
  }

  public @NonNull Stream<AbilityDescription> stream() {
    return abilities.values().stream();
  }

  @Override
  public @NonNull Iterator<AbilityDescription> iterator() {
    return Collections.unmodifiableCollection(abilities.values()).iterator();
  }
}
