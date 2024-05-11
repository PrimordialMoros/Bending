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

package me.moros.bending.common.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.data.DataKeyed;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DataProviderRegistry<T> permits DataProviderRegistryImpl {
  <V> @Nullable DataProvider<T, V> getProvider(DataKeyed<V> dataKeyed);

  default <V> @Nullable V getValue(DataKeyed<V> dataKeyed, T instance) {
    var provider = getProvider(dataKeyed);
    if (provider != null) {
      return provider.get(instance);
    }
    return null;
  }

  default <V> boolean setValue(DataKeyed<V> dataKeyed, T instance, V value) {
    var provider = getProvider(dataKeyed);
    if (provider != null) {
      return provider.set(instance, value);
    }
    return false;
  }

  default <V> boolean editValue(DataKeyed<V> dataKeyed, T instance, UnaryOperator<V> operator) {
    var provider = getProvider(dataKeyed);
    if (provider != null) {
      V oldValue = provider.get(instance);
      return oldValue != null && provider.set(instance, operator.apply(oldValue));
    }
    return false;
  }

  final class Builder<T> {
    private final Map<DataKey<?>, DataProvider<? extends T, ?>> propertyMap;
    private final Class<T> baseType;

    private Builder(Class<T> baseType) {
      this.propertyMap = new HashMap<>();
      this.baseType = baseType;
    }

    public <T1 extends T, V> Builder<T> create(DataKeyed<V> dataKeyed, Class<T1> type, Consumer<DataProviderBuilder<T, T1, V>> consumer) {
      DataProviderBuilder<T, T1, V> builder = new DataProviderBuilder<>(extractPredicate(type));
      consumer.accept(builder);
      this.propertyMap.put(dataKeyed.dataKey(), builder.validateAndBuildProvider());
      return this;
    }

    public DataProviderRegistry<T> build() {
      if (propertyMap.isEmpty()) {
        throw new IllegalStateException("No properties registered!");
      }
      return new DataProviderRegistryImpl<>(Map.copyOf(propertyMap));
    }

    private <T1 extends T> Predicate<T1> extractPredicate(Class<T1> type) {
      return type.equals(baseType) ? o -> true : type::isInstance;
    }
  }

  static <T> Builder<T> builder(Class<T> baseType) {
    Objects.requireNonNull(baseType);
    return new Builder<>(baseType);
  }
}
