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

import me.moros.bending.Bending;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.ability.sequence.SequenceStep;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds all the registered AbilityDescriptions for the current session.
 */
public final class AbilityRegistry implements Registry<AbilityDescription> {
  private final Map<String, AbilityDescription> abilities;
  private final Map<AbilityDescription, Sequence> sequences;

  AbilityRegistry() {
    abilities = new ConcurrentHashMap<>();
    sequences = new ConcurrentHashMap<>();
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
   * Register ability sequences. This must be called after all abilities have been registered.
   * Note: Some sequences may fail to register if they require a disabled or invalid ability.
   * @param sequences the map containing all the sequences
   * @return the amount of sequences that were registered.
   */
  public int register(@NonNull Map<@NonNull AbilityDescription, @NonNull Sequence> sequences) {
    int counter = 0;
    for (Map.Entry<AbilityDescription, Sequence> entry : sequences.entrySet()) {
      if (register(entry.getKey(), entry.getValue())) {
        counter++;
      }
    }
    return counter;
  }

  /**
   * Register an ability sequence. This must be called after all abilities have been registered.
   * Note: It is possible that a sequence may fail to register if it requires a disabled or invalid ability.
   * @param desc the sequence ability description
   * @param sequence the activation sequence needed for this ability
   * @return true if the sequence was registered successfully, false otherwise
   */
  public boolean register(@NonNull AbilityDescription desc, @NonNull Sequence sequence) {
    if (!sequences.containsKey(desc) && desc.isActivatedBy(Activation.SEQUENCE)) {
      boolean valid = sequence.actions().stream()
        .map(SequenceStep::ability)
        .allMatch(this::contains);
      if (valid) {
        register(desc);
        sequences.put(desc, sequence);
        return true;
      } else {
        Bending.logger().warn(desc.name() + " sequence will be disabled as it requires an invalid ability to activate.");
      }
    }
    return false;
  }

  /**
   * Check if an ability is enabled and registered.
   * @param desc the ability to check
   * @return true if the registry contains the given ability, false otherwise
   */
  public boolean contains(@NonNull AbilityDescription desc) {
    return abilities.containsKey(desc.name().toLowerCase());
  }

  /**
   * Note: this will include hidden abilities. You will need to filter them.
   * @return a stream of all the abilities in this registry
   */
  public @NonNull Stream<AbilityDescription> abilities() {
    return abilities.values().stream();
  }

  /**
   * Note: this will include hidden passives. You will need to filter them.
   * @return a stream of all the passives in this registry
   */
  public @NonNull Stream<AbilityDescription> passives() {
    return abilities().filter(desc -> desc.isActivatedBy(Activation.PASSIVE));
  }

  /**
   * Note: this will include hidden sequences. You will need to filter them.
   * @return a stream of all the sequences in this registry
   */
  public @NonNull Stream<AbilityDescription> sequences() {
    return sequences.keySet().stream();
  }

  /**
   * @param name the name to match
   * @return the ability description or null if not found
   */
  public @Nullable AbilityDescription ability(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    return abilities.get(name.toLowerCase());
  }

  /**
   * @param desc the sequence ability description
   * @return the activation sequence needed for this ability or null if not found
   */
  public @Nullable Sequence sequence(@NonNull AbilityDescription desc) {
    return sequences.get(desc);
  }

  @Override
  public @NonNull Iterator<AbilityDescription> iterator() {
    return Collections.unmodifiableCollection(abilities.values()).iterator();
  }

  public @NonNull Iterator<Map.Entry<AbilityDescription, Sequence>> sequenceIterator() {
    return Collections.unmodifiableMap(sequences).entrySet().iterator();
  }
}
