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

package me.moros.bending.common.collision;

import java.util.concurrent.atomic.AtomicBoolean;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;

record CollisionView(Ability collidedAbility, Collider colliderSelf, Collider colliderOther,
                     AtomicBoolean self, AtomicBoolean other) implements Collision {
  @Override
  public boolean removeSelf() {
    return self.get();
  }

  @Override
  public void removeSelf(boolean value) {
    self.set(value);
  }

  @Override
  public boolean removeOther() {
    return other.get();
  }

  @Override
  public void removeOther(boolean value) {
    other.set(value);
  }
}
