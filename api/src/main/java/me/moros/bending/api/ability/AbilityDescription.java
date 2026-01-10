/*
 * Copyright 2020-2026 Moros
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.api.util.functional.Suppliers;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.translation.Translatable;
import org.jspecify.annotations.Nullable;

/**
 * AbilityDescription is immutable and thread-safe.
 * Assume that all collections returning AbilityDescription are also immutable
 */
public sealed class AbilityDescription implements Keyed, Translatable permits AbilityDescription.Sequence {
  private final Key key;
  private final ElementSet elements;
  private final Component displayName;
  private final Function<AbilityDescription, ? extends Ability> constructor;
  private final EnumSet<Activation> activations;
  private final Collection<String> requiredPermissions;
  private final boolean canBind;
  private final boolean hidden;
  private final boolean bypassCooldown;
  private final int hashcode;

  private AbilityDescription(Builder builder) {
    key = builder.key;
    elements = ElementSet.copyOf(builder.elements);
    displayName = builder.displayName;
    constructor = builder.constructor;
    activations = builder.activations;
    requiredPermissions = List.copyOf(builder.requiredPermissions);
    canBind = builder.canBind && !isActivatedBy(Activation.SEQUENCE);
    hidden = builder.hidden;
    bypassCooldown = builder.bypassCooldown;
    hashcode = Objects.hash(key, elements, activations);
  }

  public Component displayName() {
    return displayName;
  }

  public Set<Element> elements() {
    return elements;
  }

  public boolean canBind() {
    return canBind;
  }

  public boolean hidden() {
    return hidden;
  }

  public boolean bypassCooldown() {
    return bypassCooldown;
  }

  public boolean isActivatedBy(Activation method) {
    return activations.contains(method);
  }

  public Ability createAbility() {
    return constructor.apply(this);
  }

  public Collection<String> permissions() {
    return requiredPermissions;
  }

  @Override
  public Key key() {
    return key;
  }

  @Override
  public String translationKey() {
    return key().namespace() + ".ability." + key().value();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbilityDescription other = (AbilityDescription) obj;
    return key.equals(other.key) && elements.equals(other.elements) && activations.equals(other.activations);
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  public static <T extends Ability> Builder builder(String name, Function<AbilityDescription, T> constructor) {
    return builder(KeyUtil.BENDING_NAMESPACE, name, constructor);
  }

  public static <T extends Ability> Builder builder(String namespace, String name, Function<AbilityDescription, T> constructor) {
    Objects.requireNonNull(namespace);
    Objects.requireNonNull(name);
    Objects.requireNonNull(constructor);
    if (namespace.isEmpty()) {
      namespace = KeyUtil.BENDING_NAMESPACE;
    }
    boolean validName = name.chars().allMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    if (name.isEmpty() || !validName) {
      throw new IllegalArgumentException("Name must be an alphabetical non-empty string.");
    }
    return new Builder(namespace, name, constructor);
  }

  /**
   * Immutable and thread-safe representation of a sequence
   */
  public static final class Sequence extends AbilityDescription {
    public static final int MAX_STEPS = 16;

    private final List<SequenceStep> steps;
    private final Supplier<Component> instructions;

    private Sequence(Builder builder, List<SequenceStep> steps) {
      super(builder);
      this.steps = List.copyOf(steps);
      this.instructions = Suppliers.lazy(this::generateInstructions);
    }

    /**
     * Get the steps required to activate this sequence.
     * @return an immutable collection of this sequence's steps
     */
    public List<SequenceStep> steps() {
      return steps;
    }

    /**
     * Get the instructions for this sequence.
     * @return the instructions
     */
    public Component instructions() {
      return instructions.get();
    }

    private Component generateInstructions() {
      TextComponent.Builder builder = Component.text();
      int size = steps.size();
      for (int i = 0; i < size; i++) {
        SequenceStep sequenceStep = steps.get(i);
        if (i != 0) {
          builder.append(Component.text(" > "));
        }
        AbilityDescription desc = sequenceStep.ability();
        Activation action = sequenceStep.activation();
        String key = action.translationKey();
        if (action == Activation.SNEAK && i + 1 < steps.size()) {
          // Check if the next instruction is to release sneak.
          SequenceStep next = steps.get(i + 1);
          if (desc.equals(next.ability()) && next.activation() == Activation.SNEAK_RELEASE) {
            key = "bending.activation.sneak-tap";
            i++;
          }
        }
        builder.append(desc.displayName()).append(Component.text(" ("))
          .append(Component.translatable(key)).append(Component.text(")"));
      }
      return builder.build();
    }

    /**
     * Check if this sequence can be activated by the provided sequence steps.
     * This method will try to match the sequence steps and fail-fast.
     * @param otherSteps the steps to match
     * @return true if this sequence can be activated by the given steps, false otherwise
     */
    public boolean matches(List<SequenceStep> otherSteps) {
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

  /**
   * Builder to create {@link AbilityDescription}.
   */
  public static final class Builder {
    private final Key key;
    private final String name;
    private Component displayName;
    private final ElementSet elements = ElementSet.mutable();
    private final Function<AbilityDescription, ? extends Ability> constructor;
    private EnumSet<Activation> activations;
    private Collection<String> requiredPermissions;
    private boolean canBind = true;
    private boolean hidden = false;
    private boolean bypassCooldown = false;

    private <T extends Ability> Builder(String namespace, String name, Function<AbilityDescription, T> constructor) {
      this.key = Key.key(namespace, name.toLowerCase(Locale.ROOT));
      this.name = name;
      this.constructor = constructor;
      this.requiredPermissions = List.of(defaultPermission());
    }

    public Builder displayName(Component displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder element(Element element) {
      this.elements.add(element);
      return this;
    }

    public Builder element(Element first, Element @Nullable ... elements) {
      this.elements.add(first);
      if (elements != null) {
        this.elements.addAll(List.of(elements));
      }
      return this;
    }

    public Builder activation(Activation method, Activation @Nullable ... methods) {
      Collection<Activation> c = new ArrayList<>();
      if (methods != null) {
        c.addAll(List.of(methods));
      }
      c.add(method);
      activations = EnumSet.copyOf(c);
      return this;
    }

    public Builder require(String @Nullable ... permissions) {
      Collection<String> c = new LinkedHashSet<>();
      c.add(defaultPermission());
      if (permissions != null) {
        c.addAll(List.of(permissions));
      }
      requiredPermissions = c;
      return this;
    }

    public Builder canBind(boolean canBind) {
      this.canBind = canBind;
      return this;
    }

    public Builder hidden(boolean hidden) {
      this.hidden = hidden;
      return this;
    }

    public Builder bypassCooldown(boolean bypassCooldown) {
      this.bypassCooldown = bypassCooldown;
      return this;
    }

    public AbilityDescription build() {
      validate();
      if (activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Can't build sequence");
      }
      return new AbilityDescription(this);
    }

    public Sequence buildSequence(UnaryOperator<SequenceBuilder> function) {
      validate();
      if (!activations.contains(Activation.SEQUENCE)) {
        throw new IllegalStateException("Ability must be activated by sequence");
      }
      List<SequenceStep> sequenceSteps = function.apply(new SequenceBuilder()).validateAndBuild();
      return new Sequence(this, sequenceSteps);
    }

    private void validate() {
      if (elements.isEmpty()) {
        throw new IllegalStateException("Elements cannot be empty");
      }
      Objects.requireNonNull(activations, "Activations cannot be null");
      if (activations.isEmpty()) {
        throw new IllegalStateException("Activation methods cannot be empty");
      }
      if (displayName == null) {
        TextColor color = elements.size() > 2 ? ColorPalette.AVATAR : elements.iterator().next().color();
        displayName = Component.text(name, color);
      }
    }

    private String defaultPermission() {
      return key.namespace() + ".ability." + key.value();
    }
  }
}
