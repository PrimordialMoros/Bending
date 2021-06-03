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

package me.moros.bending.model.predicate.removal;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SwappedSlotsRemovalPolicy implements RemovalPolicy {
  private final AbilityDescription expected;

  private SwappedSlotsRemovalPolicy(AbilityDescription expected) {
    this.expected = expected;
  }

  @Override
  public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
    if (!(user instanceof BendingPlayer)) {
      return false;
    }
    return !expected.equals(user.selectedAbility());
  }

  public static @NonNull RemovalPolicy of(@NonNull AbilityDescription expected) {
    return new SwappedSlotsRemovalPolicy(expected);
  }
}
