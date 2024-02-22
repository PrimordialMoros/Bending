/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription.Sequence;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility to link multiple {@link SequenceStep}.
 */
public final class SequenceBuilder {
  private final List<SequenceStep> steps;

  SequenceBuilder() {
    this.steps = new ArrayList<>();
  }

  public SequenceBuilder add(AbilityDescription ability, Activation activation) {
    Objects.requireNonNull(ability);
    Objects.requireNonNull(activation);
    if (steps.size() > Sequence.MAX_STEPS) {
      throw new IllegalStateException("Cannot add more than %d steps!".formatted(Sequence.MAX_STEPS));
    }
    if (!ability.canBind()) {
      throw new IllegalArgumentException("%s cannot be used as a sequence activation step!".formatted(ability.key().asString()));
    }
    if (activation == Activation.PASSIVE || activation == Activation.SEQUENCE) {
      throw new IllegalArgumentException("%s cannot be used for sequence activation!".formatted(activation.name()));
    }
    this.steps.add(SequenceStep.of(ability, activation));
    return this;
  }

  public SequenceBuilder add(AbilityDescription ability, Activation activation, Activation @Nullable ... activations) {
    add(ability, activation);
    if (activations != null) {
      for (Activation temp : activations) {
        add(ability, temp);
      }
    }
    return this;
  }

  List<SequenceStep> validateAndBuild() {
    if (steps.size() < 2) {
      throw new IllegalStateException("Sequences require at least 2 activation steps!");
    }
    return List.copyOf(steps);
  }
}
