/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.common.collision;

import java.util.concurrent.atomic.AtomicBoolean;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;

public final class CollisionData {
  private final Ability first, second;
  private final Collider c1, c2;
  private final AtomicBoolean removeFirst, removeSecond;

  public CollisionData(Ability first, Ability second, Collider c1, Collider c2, boolean removeFirst, boolean removeSecond) {
    this.first = first;
    this.second = second;
    this.c1 = c1;
    this.c2 = c2;
    this.removeFirst = new AtomicBoolean(removeFirst);
    this.removeSecond = new AtomicBoolean(removeSecond);
  }

  public boolean removeFirst() {
    return removeFirst.get();
  }

  public boolean removeSecond() {
    return removeSecond.get();
  }

  public Collision asCollision() {
    return new CollisionView(second, c1, c2, removeFirst, removeSecond);
  }

  public Collision asInverseCollision() {
    return new CollisionView(first, c2, c1, removeSecond, removeFirst);
  }
}
