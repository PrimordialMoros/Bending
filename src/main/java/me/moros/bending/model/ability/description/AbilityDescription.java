/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.model.ability.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.util.ColorPalette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * AbilityDescription is immutable and thread-safe.
 * Assume that all collections returning AbilityDescription are also immutable
 */
public class AbilityDescription {
  private final String name;
  private final Function<AbilityDescription, ? extends Ability> constructor;
  private final Element element;
  private final EnumSet<Activation> activations;
  private final boolean hidden;
  private final boolean canBind;
  private final boolean sourcePlant;
  private final boolean bypassCooldown;
  private final int hashcode;

  private AbilityDescription(AbilityDescriptionBuilder builder) {
    name = builder.name;
    constructor = builder.constructor;
    element = builder.element;
    activations = builder.activations;
    canBind = builder.canBind && !isActivatedBy(Activation.SEQUENCE);
    hidden = builder.hidden;
    sourcePlant = builder.sourcePlant;
    bypassCooldown = builder.bypassCooldown;
    hashcode = Objects.hash(name, constructor, element, activations, hidden, canBind, sourcePlant, bypassCooldown);
    createAbility(); // Init config values
  }

  public @NonNull String name() {
    return name;
  }

  public @NonNull Component displayName() {
    return Component.text(name, element.color());
  }

  public @NonNull Element element() {
    return element;
  }

  public boolean canBind() {
    return canBind;
  }

  public boolean hidden() {
    return hidden;
  }

  public boolean sourcePlant() {
    return sourcePlant;
  }

  public boolean bypassCooldown() {
    return bypassCooldown;
  }

  public boolean isActivatedBy(@NonNull Activation method) {
    return activations.contains(method);
  }

  public @NonNull Ability createAbility() {
    return constructor.apply(this);
  }

  public @NonNull String permission() {
    return "bending.ability." + name;
  }

  public @NonNull Component meta() {
    String type = "ability";
    if (isActivatedBy(Activation.PASSIVE)) {
      type = "passive";
    } else if (isActivatedBy(Activation.SEQUENCE)) {
      type = "sequence";
    }
    Component details = Component.text()
      .append(Component.text(element() + " " + type, element.color()))
      .append(Component.newline())
      .append(Component.text("Click to view info about this " + type + ".", ColorPalette.NEUTRAL)).build();

    return displayName()
      .hoverEvent(HoverEvent.showText(details))
      .clickEvent(ClickEvent.runCommand("/bending help " + name()));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbilityDescription desc = (AbilityDescription) obj;
    return name().equals(desc.name()) && element() == desc.element();
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  public static <T extends Ability> @NonNull AbilityDescriptionBuilder builder(@NonNull String name, @NonNull Function<AbilityDescription, T> constructor) {
    return new AbilityDescriptionBuilder(name, constructor);
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

  /**
   * Immutable and thread-safe representation of a sequence
   */
  public static final class Sequence extends AbilityDescription {
    private final List<SequenceStep> steps;
    private Component instructions;

    private Sequence(AbilityDescriptionBuilder builder, List<SequenceStep> steps) {
      super(builder);
      this.steps = List.copyOf(steps);
    }

    public @NonNull List<@NonNull SequenceStep> steps() {
      return steps;
    }

    public @NonNull Component instructions() {
      if (instructions == null) {
        instructions = generateInstructions(steps);
      }
      return instructions;
    }

    public boolean matches(@NonNull List<@NonNull SequenceStep> otherSteps) {
      int actionsLength = otherSteps.size() - 1;
      int sequenceLength = steps.size() - 1;
      if (actionsLength < sequenceLength) {
        return false;
      }
      for (int i = 0; i <= sequenceLength; i++) {
        SequenceStep first = steps.get(sequenceLength - i);
        SequenceStep second = otherSteps.get(actionsLength - i);
        if (!first.equals(second)) {
          return false;
        }
      }
      return true;
    }
  }

  public static class AbilityDescriptionBuilder {
    private final String name;
    private final Function<AbilityDescription, ? extends Ability> constructor;
    private Element element;
    private EnumSet<Activation> activations;
    private boolean canBind = true;
    private boolean hidden = false;
    private boolean sourcePlant = false;
    private boolean bypassCooldown = false;

    public <T extends Ability> AbilityDescriptionBuilder(@NonNull String name, @NonNull Function<@NonNull AbilityDescription, @NonNull T> constructor) {
      this.name = name;
      this.constructor = constructor;
    }

    public @NonNull AbilityDescriptionBuilder element(@NonNull Element element) {
      this.element = element;
      return this;
    }

    public @NonNull AbilityDescriptionBuilder activation(@NonNull Activation method, @Nullable Activation... methods) {
      Collection<Activation> c = new ArrayList<>();
      if (methods != null) {
        c.addAll(List.of(methods));
      }
      c.add(method);
      activations = EnumSet.copyOf(c);
      return this;
    }

    public @NonNull AbilityDescriptionBuilder canBind(boolean canBind) {
      this.canBind = canBind;
      return this;
    }

    public @NonNull AbilityDescriptionBuilder hidden(boolean hidden) {
      this.hidden = hidden;
      return this;
    }

    public @NonNull AbilityDescriptionBuilder sourcePlant(boolean sourcePlant) {
      this.sourcePlant = sourcePlant;
      return this;
    }

    public @NonNull AbilityDescriptionBuilder bypassCooldown(boolean bypassCooldown) {
      this.bypassCooldown = bypassCooldown;
      return this;
    }

    public @NonNull AbilityDescription build() {
      validate();
      if (activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Can't build sequence");
      }
      return new AbilityDescription(this);
    }

    public @NonNull Sequence buildSequence(@NonNull SequenceStep step1, @NonNull SequenceStep step2, @NonNull SequenceStep... steps) {
      validate();
      if (!activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Ability must be activated by sequence");
      }
      List<SequenceStep> sequenceSteps = new ArrayList<>();
      sequenceSteps.add(Objects.requireNonNull(step1, "Sequence steps cannot be null"));
      sequenceSteps.add(Objects.requireNonNull(step2, "Sequence steps cannot be null"));
      if (steps != null) {
        sequenceSteps.addAll(List.of(steps));
      }
      return new Sequence(this, sequenceSteps);
    }

    private void validate() {
      Objects.requireNonNull(element, "Element cannot be null");
      Objects.requireNonNull(activations, "Activations cannot be null");
      if (activations.isEmpty()) {
        throw new IllegalStateException("Activation methods cannot be empty");
      }
    }
  }
}
