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

package me.moros.bending.model.predicate.general;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.util.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.NonNull;

public enum BendingConditions implements BendingConditional {
  COOLDOWN((u, d) -> (!u.onCooldown(d))),
  ELEMENT((u, d) -> u.hasElement(d.element())),
  GAMEMODE((u, d) -> !u.spectator()),
  PERMISSION((u, d) -> u.hasPermission(d)),
  DISABLED((u, d) -> !u.entity().hasMetadata(Metadata.DISABLED)),
  WORLD((u, d) -> !u.world().hasMetadata(Metadata.DISABLED));

  private final BendingConditional predicate;

  BendingConditions(BendingConditional predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
    return predicate.test(user, desc);
  }

  /**
   * Constructs a new builder that includes {@link BendingConditions#COOLDOWN},
   * {@link BendingConditions#ELEMENT}, {@link BendingConditions#GAMEMODE},
   * {@link BendingConditions#WORLD} and {@link BendingConditions#PERMISSION}.
   */
  public static @NonNull Builder builder() {
    return new Builder()
      .add(BendingConditions.COOLDOWN)
      .add(BendingConditions.ELEMENT)
      .add(BendingConditions.GAMEMODE)
      .add(BendingConditions.WORLD)
      .add(BendingConditions.DISABLED)
      .add(BendingConditions.PERMISSION);
  }

  public static final class Builder {
    private final Set<BendingConditional> conditionals;

    private Builder() {
      conditionals = new HashSet<>();
    }

    public @NonNull Builder add(@NonNull BendingConditional conditional) {
      conditionals.add(conditional);
      return this;
    }

    public @NonNull Builder remove(@NonNull BendingConditional conditional) {
      conditionals.remove(conditional);
      return this;
    }

    public @NonNull BendingConditional build() {
      return new CompositeBendingConditional(this);
    }

    @NonNull Set<@NonNull BendingConditional> conditionals() {
      return conditionals;
    }
  }

  private static class CompositeBendingConditional implements BendingConditional {
    private final Set<BendingConditional> conditionals;

    CompositeBendingConditional(@NonNull Builder builder) {
      this.conditionals = Set.copyOf(builder.conditionals());
    }

    @Override
    public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
      Predicate<BendingConditional> filter = desc.bypassCooldown() ? c -> !c.equals(BendingConditions.COOLDOWN) : c -> true;
      return conditionals.stream().filter(filter).allMatch(cond -> cond.test(user, desc));
    }
  }
}
