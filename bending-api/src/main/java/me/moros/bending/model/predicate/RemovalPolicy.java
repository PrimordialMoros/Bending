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

package me.moros.bending.model.predicate;

import java.util.Objects;
import java.util.function.BiPredicate;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;

/**
 * BiPredicate to test whether a user's ability should be removed.
 */
@FunctionalInterface
public interface RemovalPolicy extends BiPredicate<User, AbilityDescription> {
  @Override
  default RemovalPolicy and(BiPredicate<? super User, ? super AbilityDescription> other) {
    Objects.requireNonNull(other);
    return (u, d) -> test(u, d) && other.test(u, d);
  }

  @Override
  default RemovalPolicy or(BiPredicate<? super User, ? super AbilityDescription> other) {
    Objects.requireNonNull(other);
    return (u, d) -> test(u, d) || other.test(u, d);
  }

  @Override
  default RemovalPolicy negate() {
    return (u, d) -> !test(u, d);
  }
}
