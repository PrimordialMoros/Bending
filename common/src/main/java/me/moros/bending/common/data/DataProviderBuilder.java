/*
 * Copyright 2020-2025 Moros
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

public final class DataProviderBuilder<T, T1 extends T, V> {
  private final Predicate<T1> predicate;
  private @Nullable UnaryOperator<V> operator;
  private @Nullable Function<T1, V> getter;
  private @Nullable BiConsumer<T1, V> setter;

  DataProviderBuilder(Predicate<T1> predicate) {
    this.predicate = predicate;
  }

  public DataProviderBuilder<T, T1, V> valueOperator(UnaryOperator<V> operator) {
    this.operator = operator;
    return this;
  }

  public DataProviderBuilder<T, T1, V> get(Function<T1, V> getter) {
    this.getter = getter;
    return this;
  }

  public DataProviderBuilder<T, T1, V> set(BiConsumer<T1, V> setter) {
    this.setter = setter;
    return this;
  }

  DataProvider<T1, V> validateAndBuildProvider() {
    if (getter == null && setter == null) {
      throw new IllegalStateException("Cannot build empty data provider.");
    }
    return new DataProviderImpl<>(predicate, operator, getter, setter);
  }
}
