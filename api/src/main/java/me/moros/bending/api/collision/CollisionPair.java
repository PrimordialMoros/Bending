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

package me.moros.bending.api.collision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

/**
 * Represents a possible collision between 2 abilities.
 */
public final class CollisionPair implements Keyed {
  private final AbilityDescription first;
  private final AbilityDescription second;
  private final boolean removeFirst;
  private final boolean removeSecond;
  private final Key key;

  CollisionPair(AbilityDescription first, AbilityDescription second, boolean removeFirst, boolean removeSecond) {
    this.first = first;
    this.second = second;
    this.removeFirst = removeFirst;
    this.removeSecond = removeSecond;
    this.key = createKey(first, second);
  }

  public AbilityDescription first() {
    return first;
  }

  public AbilityDescription second() {
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
    CollisionPair other = (CollisionPair) obj;
    return (first.equals(other.first) && second.equals(other.second)) || (first.equals(other.second) && second.equals(other.first));
  }

  private int hashcode;

  @Override
  public int hashCode() {
    if (hashcode == 0) {
      int maxHash = Math.max(first.hashCode(), second.hashCode());
      int minHash = Math.min(first.hashCode(), second.hashCode());
      hashcode = 31 * minHash + maxHash;
    }
    return hashcode;
  }

  @Override
  public String toString() {
    return first.key().asString() + " (Remove: " + removeFirst + ") - " +
      second.key().asString() + "(Remove: " + removeSecond + ")";
  }

  public static Key createKey(AbilityDescription first, AbilityDescription second) {
    String f = KeyUtil.concat(first.key());
    String s = KeyUtil.concat(second.key());
    String value = f.compareTo(s) > 0 ? (f + '-' + s) : (s + '-' + f);
    return KeyUtil.simple(value);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Key key() {
    return key;
  }

  /**
   * A builder to easily register collisions based on a layer system.
   * Normally, abilities that belong to the same layer will cancel each other out.
   * Moreover, they will remove all abilities in the layers below them and be removed by all abilities in layers above them.
   */
  public static final class Builder {
    private final List<CollisionLayer> layers;
    private final Collection<CollisionPair> simpleCollisions;

    private Builder() {
      layers = new ArrayList<>();
      simpleCollisions = new ArrayList<>();
    }

    public Builder layer(Iterable<Key> abilities) {
      layers.add(new CollisionLayer(mapAbilities(abilities), true));
      return this;
    }

    public Builder add(Key first, Key second, boolean removeFirst, boolean removeSecond) {
      return add(List.of(first), List.of(second), removeFirst, removeSecond);
    }

    public Builder add(Key first, Iterable<Key> second, boolean removeFirst, boolean removeSecond) {
      return add(List.of(first), second, removeFirst, removeSecond);
    }

    public Builder add(Iterable<Key> first, Iterable<Key> second, boolean removeFirst, boolean removeSecond) {
      for (AbilityDescription desc1 : mapAbilities(first)) {
        for (AbilityDescription desc2 : mapAbilities(second)) {
          simpleCollisions.add(new CollisionPair(desc1, desc2, removeFirst, removeSecond));
        }
      }
      return this;
    }

    public Collection<CollisionPair> build() {
      Set<CollisionPair> collisionSet = new HashSet<>(simpleCollisions);
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
              collisionSet.add(new CollisionPair(first, second, true, false));
            }
          }
        }
      }
      return collisionSet;
    }

    private List<AbilityDescription> mapAbilities(Iterable<Key> abilities) {
      return StreamSupport.stream(abilities.spliterator(), false).map(Registries.ABILITIES::getOrThrow).toList();
    }

    private static Collection<CollisionPair> registerSelfCancellingCollisions(List<AbilityDescription> layer) {
      Collection<CollisionPair> tempCollisions = new ArrayList<>();
      int size = layer.size();
      for (int i = 0; i < size; i++) {
        for (int j = i; j < size; j++) {
          tempCollisions.add(new CollisionPair(layer.get(i), layer.get(j), true, true));
        }
      }
      return tempCollisions;
    }
  }

  private record CollisionLayer(List<AbilityDescription> layerAbilities, boolean interCollisions) {
  }
}
