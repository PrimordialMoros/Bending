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

package me.moros.bending.registry;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.ability.description.SequenceStep;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Holds all the registered AbilityDescriptions for the current session.
 */
public final class SequenceRegistry implements Registry<Sequence> {
  private final Set<Sequence> sequences;

  SequenceRegistry() {
    sequences = ConcurrentHashMap.newKeySet();
  }

  /**
   * Register ability sequences. This must be called after all abilities have been registered.
   * Note: Some sequences may fail to register if they require a disabled or invalid ability.
   * @param sequences the map containing all the sequences
   * @return the amount of sequences that were registered.
   */
  public int register(@NonNull Iterable<@NonNull Sequence> sequences) {
    int counter = 0;
    for (Sequence sequence : sequences) {
      if (register(sequence)) {
        counter++;
      }
    }
    return counter;
  }

  /**
   * Register an ability sequence. This must be called after all abilities have been registered.
   * Note: It is possible that a sequence may fail to register if it requires a disabled or invalid ability.
   * @param sequence the activation sequence needed for this ability
   * @return true if the sequence was registered successfully, false otherwise
   */
  public boolean register(@NonNull Sequence sequence) {
    if (!contains(sequence)) {
      if (sequence.steps().stream().map(SequenceStep::ability).allMatch(Registries.ABILITIES::contains)) {
        Registries.ABILITIES.register(sequence);
        sequences.add(sequence);
        return true;
      }
    }
    return false;
  }

  public boolean contains(@NonNull Sequence sequence) {
    return sequences.contains(sequence);
  }

  /**
   * Note: this will include hidden sequences. You will need to filter them.
   * @return a stream of all the sequences in this registry
   */
  public @NonNull Stream<Sequence> stream() {
    return sequences.stream();
  }

  @Override
  public @NonNull Iterator<Sequence> iterator() {
    return Collections.unmodifiableSet(sequences).iterator();
  }
}
