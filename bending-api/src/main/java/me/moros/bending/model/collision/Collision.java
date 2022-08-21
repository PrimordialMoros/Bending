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

package me.moros.bending.model.collision;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.collision.geometry.Collider;

/**
 * Represents a real collision between two ability instances.
 */
public final class Collision {
  private final CollisionData collisionData;
  private final boolean inverse;

  private Collision(CollisionData collisionData, boolean inverse) {
    this.collisionData = collisionData;
    this.inverse = inverse;
  }

  public Ability collidedAbility() {
    return inverse ? collisionData.first : collisionData.second;
  }

  public Collider colliderSelf() {
    return inverse ? collisionData.c2 : collisionData.c1;
  }

  public Collider colliderOther() {
    return inverse ? collisionData.c1 : collisionData.c2;
  }

  public boolean removeSelf() {
    return inverse ? collisionData.removeSecond : collisionData.removeFirst;
  }

  public void removeSelf(boolean value) {
    if (inverse) {
      collisionData.removeSecond = value;
    } else {
      collisionData.removeFirst = value;
    }
  }

  public boolean removeOther() {
    return inverse ? collisionData.removeFirst : collisionData.removeSecond;
  }

  public void removeOther(boolean value) {
    if (inverse) {
      collisionData.removeFirst = value;
    } else {
      collisionData.removeSecond = value;
    }
  }

  /**
   * Holds the data for a collision between two ability instances.
   */
  public static class CollisionData {
    private final Ability first, second;
    private final Collider c1, c2;
    private boolean removeFirst, removeSecond;

    public CollisionData(Ability first, Ability second, Collider c1, Collider c2, boolean removeFirst, boolean removeSecond) {
      this.first = first;
      this.second = second;
      this.c1 = c1;
      this.c2 = c2;
      this.removeFirst = removeFirst;
      this.removeSecond = removeSecond;
    }

    public boolean removeFirst() {
      return removeFirst;
    }

    public boolean removeSecond() {
      return removeSecond;
    }

    public Collision asCollision() {
      return new Collision(this, false);
    }

    public Collision asInverseCollision() {
      return new Collision(this, true);
    }
  }
}
