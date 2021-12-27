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

package me.moros.bending.model.collision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a possible collision between 2 abilities.
 */
public final class RegisteredCollision {
  private final AbilityDescription first;
  private final AbilityDescription second;
  private final boolean removeFirst;
  private final boolean removeSecond;

  RegisteredCollision(@NonNull AbilityDescription first, @NonNull AbilityDescription second, boolean removeFirst, boolean removeSecond) {
    this.first = first;
    this.second = second;
    this.removeFirst = removeFirst;
    this.removeSecond = removeSecond;
  }

  public @NonNull AbilityDescription first() {
    return first;
  }

  public @NonNull AbilityDescription second() {
    return second;
  }

  public boolean removeFirst() {
    return removeFirst;
  }

  public boolean removeSecond() {
    return removeSecond;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RegisteredCollision that = (RegisteredCollision) obj;
    return (first.equals(that.first) && second.equals(that.second)) || (first.equals(that.second) && second.equals(that.first));
  }

  @Override
  public int hashCode() {
    int maxHash = Math.max(first.hashCode(), second.hashCode());
    int minHash = Math.min(first.hashCode(), second.hashCode());
    return minHash * 31 + maxHash;
  }

  @Override
  public String toString() {
    return first.name() + " (Remove: " + removeFirst + ") - " + second.name() + "(Remove: " + removeSecond + ")";
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * A builder to easily register collisions based on a layer system.
   * Normally, abilities that belong to the same layer will cancel each other out.
   * Moreover, they will remove all abilities in the layers below them and be removed by all abilities in layers above them.
   */
  public static class Builder {
    private final List<CollisionLayer> layers;
    private final Collection<RegisteredCollision> simpleCollisions;

    private Builder() {
      layers = new ArrayList<>();
      simpleCollisions = new ArrayList<>();
    }

    public @NonNull Builder layer(@NonNull Collection<@NonNull String> abilities) {
      layers.add(new CollisionLayer(mapAbilities(abilities), true));
      return this;
    }

    public @NonNull Builder add(@NonNull String first, @NonNull String second, boolean removeFirst, boolean removeSecond) {
      return add(List.of(first), List.of(second), removeFirst, removeSecond);
    }

    public @NonNull Builder add(@NonNull String first, @NonNull Collection<@NonNull String> second, boolean removeFirst, boolean removeSecond) {
      return add(List.of(first), second, removeFirst, removeSecond);
    }

    public @NonNull Builder add(@NonNull Collection<@NonNull String> first, @NonNull Collection<@NonNull String> second, boolean removeFirst, boolean removeSecond) {
      for (AbilityDescription desc1 : mapAbilities(first)) {
        for (AbilityDescription desc2 : mapAbilities(second)) {
          simpleCollisions.add(new RegisteredCollision(desc1, desc2, removeFirst, removeSecond));
        }
      }
      return this;
    }

    public @NonNull Collection<@NonNull RegisteredCollision> build() {
      Set<RegisteredCollision> collisionSet = new HashSet<>(simpleCollisions);
      int size = layers.size();
      for (int i = 0; i < size; i++) {
        CollisionLayer currentLayer = layers.get(i);
        if (currentLayer.interCollisions) {
          collisionSet.addAll(registerSelfCancellingCollisions(currentLayer.layerAbilities));
        }
        for (int j = i + 1; j < size; j++) {
          CollisionLayer layerAbove = layers.get(j);
          for (AbilityDescription first : currentLayer.layerAbilities) {
            for (AbilityDescription second : layerAbove.layerAbilities) {
              collisionSet.add(new RegisteredCollision(first, second, true, false));
            }
          }
        }
      }
      return new ArrayList<>(collisionSet);
    }

    private List<AbilityDescription> mapAbilities(Collection<String> abilities) {
      return abilities.stream().map(Registries.ABILITIES::ability).filter(Objects::nonNull).toList();
    }

    private static Collection<RegisteredCollision> registerSelfCancellingCollisions(List<AbilityDescription> layer) {
      Collection<RegisteredCollision> tempCollisions = new ArrayList<>();
      int size = layer.size();
      for (int i = 0; i < size; i++) {
        for (int j = i; j < size; j++) {
          tempCollisions.add(new RegisteredCollision(layer.get(i), layer.get(j), true, true));
        }
      }
      return tempCollisions;
    }
  }

  private record CollisionLayer(List<AbilityDescription> layerAbilities, boolean interCollisions) {
  }
}
