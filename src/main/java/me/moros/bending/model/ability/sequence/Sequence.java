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

package me.moros.bending.model.ability.sequence;

import java.util.ArrayList;
import java.util.List;

import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Immutable and thread-safe representation of a sequence
 */
public final class Sequence {
  private final List<SequenceStep> sequence = new ArrayList<>();
  private Component instructions;

  public Sequence(@NonNull SequenceStep action, @NonNull SequenceStep... actions) {
    this.sequence.add(action);
    this.sequence.addAll(List.of(actions));
  }

  /**
   * @return Unmodifiable view of this sequence's actions
   */
  public @NonNull List<@NonNull SequenceStep> actions() {
    return List.copyOf(sequence);
  }

  public @NonNull Component instructions() {
    if (instructions == null) {
      instructions = generateInstructions(sequence);
    }
    return instructions;
  }

  private static Component generateInstructions(List<SequenceStep> actions) {
    TextComponent.Builder builder = Component.text();
    for (int i = 0; i < actions.size(); i++) {
      SequenceStep sequenceStep = actions.get(i);
      if (i != 0) {
        builder.append(Component.text(" > "));
      }
      AbilityDescription desc = sequenceStep.ability();
      Activation action = sequenceStep.activation();
      String actionKey = action.key();
      if (action == Activation.SNEAK && i + 1 < actions.size()) {
        // Check if the next instruction is to release sneak.
        SequenceStep next = actions.get(i + 1);
        if (desc.equals(next.ability()) && next.activation() == Activation.SNEAK_RELEASE) {
          actionKey = "bending.activation.sneak-tap";
          i++;
        }
      }
      builder.append(Component.text(desc.name())).append(Component.text(" ("))
        .append(Component.translatable(actionKey)).append(Component.text(")"));
    }
    return builder.build();
  }

  public boolean matches(@NonNull List<@NonNull SequenceStep> actions) {
    int actionsLength = actions.size() - 1;
    int sequenceLength = sequence.size() - 1;
    if (actionsLength < sequenceLength) {
      return false;
    }
    for (int i = 0; i <= sequenceLength; i++) {
      SequenceStep first = sequence.get(sequenceLength - i);
      SequenceStep second = actions.get(actionsLength - i);
      if (!first.equals(second)) {
        return false;
      }
    }
    return true;
  }
}
