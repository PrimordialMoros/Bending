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

package me.moros.bending.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.collision.CollisionPair;
import me.moros.bending.model.key.Key;
import me.moros.bending.model.key.KeyValidator;
import me.moros.bending.model.key.Keyed;
import me.moros.bending.model.key.Namespaced;
import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.registry.Registry;
import me.moros.bending.model.registry.RegistryBuilder;
import me.moros.bending.protection.Protection;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unchecked")
public final class Registries {
  private static final Map<RegistryKey<? extends Keyed>, Registry<Key, ? extends Keyed>> REGISTRIES_BY_KEY = new HashMap<>();
  private static final Map<Class<? extends Keyed>, Registry<Key, ? extends Keyed>> REGISTRIES_BY_CLASS = new HashMap<>();
  private static final Collection<RegistryKey<? extends Keyed>> REGISTRY_KEYS = new HashSet<>();

  public static final Registry<Key, AbilityDescription> ABILITIES = create(AbilityDescription.NAMESPACE, AbilityDescription.class);
  public static final Registry<Key, Sequence> SEQUENCES = create(Sequence.NAMESPACE, Sequence.class);
  public static final Registry<Key, CollisionPair> COLLISIONS = create(CollisionPair.NAMESPACE, CollisionPair.class);
  public static final Registry<Key, Protection> PROTECTIONS = create(Protection.NAMESPACE, Protection.class);
  public static final UserRegistry BENDERS = new UserRegistry();

  private Registries() {
  }

  public static Stream<RegistryKey<? extends Keyed>> keys() {
    return REGISTRY_KEYS.stream();
  }

  public static <T extends Keyed> Registry<Key, T> get(RegistryKey<T> type) {
    return (Registry<Key, T>) REGISTRIES_BY_KEY.get(type);
  }

  public static <T extends Keyed> @Nullable Registry<Key, T> get(Class<T> type) {
    return (Registry<Key, T>) REGISTRIES_BY_CLASS.get(type);
  }

  private static <T extends Keyed> Registry<Key, T> create(String namespace, Class<T> clazz) {
    RegistryKey<T> type = RegistryKey.create(namespace, clazz);
    REGISTRY_KEYS.add(type);
    Registry<Key, T> registry = RegistryBuilder.builder(type).inverseMapper(Keyed::key)
      .keyMapper(stringToKey(type)).build();
    REGISTRIES_BY_KEY.put(type, registry);
    REGISTRIES_BY_CLASS.put(clazz, registry);
    return registry;
  }

  private static Function<String, Key> stringToKey(Namespaced namespaced) {
    final String namespace = namespaced.namespace();
    return input -> {
      String lowerCaseKey = input.toLowerCase(Locale.ROOT);
      return KeyValidator.isValidString(lowerCaseKey) ? Key.create(namespace, lowerCaseKey) : null;
    };
  }
}
