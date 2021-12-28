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

package me.moros.bending.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import me.moros.bending.model.collision.RegisteredCollision;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CollisionRegistry implements Registry<RegisteredCollision> {
  private final Set<RegisteredCollision> collisions;

  CollisionRegistry() {
    collisions = new HashSet<>();
  }

  public boolean contains(@NonNull RegisteredCollision collision) {
    return collisions.contains(collision);
  }

  public int register(@NonNull Iterable<@NonNull RegisteredCollision> collisions) {
    int counter = 0;
    for (RegisteredCollision collision : collisions) {
      if (register(collision)) {
        counter++;
      }
    }
    return counter;
  }

  public boolean register(@NonNull RegisteredCollision collision) {
    return collisions.add(collision);
  }

  public boolean unregister(@NonNull RegisteredCollision collision) {
    return collisions.remove(collision);
  }

  public @NonNull Iterator<RegisteredCollision> iterator() {
    return Collections.unmodifiableSet(collisions).iterator();
  }
}
