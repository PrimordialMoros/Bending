/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.collision;

import me.moros.bending.model.ability.description.AbilityDescription;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a possible collision between 2 abilities.
 */
public final class RegisteredCollision {
  private final AbilityDescription first;
  private final AbilityDescription second;
  private final boolean removeFirst;
  private final boolean removeSecond;

  public RegisteredCollision(@NonNull AbilityDescription first, @NonNull AbilityDescription second, boolean removeFirst, boolean removeSecond) {
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
}
