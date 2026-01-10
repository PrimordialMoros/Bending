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

package me.moros.bending.common.data;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

record DataProviderImpl<T, V>(Predicate<T> supportPredicate,
                              @Nullable UnaryOperator<V> operator,
                              @Nullable Function<T, V> getter,
                              @Nullable BiConsumer<T, V> setter) implements DataProvider<T, V> {
  @Override
  public boolean supports(T instance) {
    return supportPredicate.test(instance);
  }

  @Override
  public @Nullable V get(T instance) {
    return getter != null && supports(instance) ? getter.apply(instance) : null;
  }

  @Override
  public boolean set(T instance, V value) {
    if (setter != null && supports(instance)) {
      setter.accept(instance, operator == null ? value : operator.apply(value));
      return true;
    }
    return false;
  }
}
