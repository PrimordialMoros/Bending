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

package me.moros.bending.sponge.platform.entity;

import java.util.function.Function;

import me.moros.bending.common.data.DataProviderBuilder;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;

final class PropertyMapper {
  private PropertyMapper() {
  }

  static <T1 extends Entity, V> void readOnly(DataProviderBuilder<Entity, T1, V> builder,
                                              Key<Value<V>> key) {
    new Getter<T1, V, V>(key, Function.identity()).accept(builder);
  }

  static <T1 extends Entity, V> void property(DataProviderBuilder<Entity, T1, V> builder,
                                              Key<Value<V>> key) {
    new GetterSetter<T1, V, V>(key, Function.identity(), Function.identity()).accept(builder);
  }

  static <T1 extends Entity, V1, V> void readOnly(DataProviderBuilder<Entity, T1, V> builder,
                                                  Key<Value<V1>> key,
                                                  Function<V1, V> getterMapper) {
    new Getter<T1, V, V1>(key, getterMapper).accept(builder);
  }

  static <T1 extends Entity, V1, V> void property(DataProviderBuilder<Entity, T1, V> builder,
                                                  Key<Value<V1>> key,
                                                  Function<V1, V> getterMapper,
                                                  Function<V, V1> setterMapper) {
    new GetterSetter<T1, V, V1>(key, getterMapper, setterMapper).accept(builder);
  }

  private record Getter<T extends Entity, V, V1>(Key<Value<V1>> key,
                                                 Function<V1, V> getterMapper) {
    private void accept(DataProviderBuilder<Entity, T, V> builder) {
      builder.get(e -> e.get(key).map(getterMapper).orElseThrow());
    }
  }

  private record GetterSetter<T extends Entity, V, V1>(Key<Value<V1>> key,
                                                       Function<V1, V> getterMapper,
                                                       Function<V, V1> setterMapper) {
    private void accept(DataProviderBuilder<Entity, T, V> builder) {
      builder.get(e -> e.get(key).map(getterMapper).orElseThrow()).set((e, v) -> e.offer(key, setterMapper.apply(v)));
    }
  }
}
