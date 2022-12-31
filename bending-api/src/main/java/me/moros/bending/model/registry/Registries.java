/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.model.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import me.moros.bending.locale.Translation;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityDescription.Sequence;
import me.moros.bending.model.collision.CollisionPair;
import me.moros.bending.model.protection.Protection;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds all the built-in registries.
 */
public final class Registries {
  private static final Map<Key, Registry<Key, ? extends Keyed>> REGISTRIES_BY_KEY = new HashMap<>();
  private static final Map<Class<? extends Keyed>, Registry<Key, ? extends Keyed>> REGISTRIES_BY_CLASS = new HashMap<>();
  private static final Collection<Key> REGISTRY_KEYS = new HashSet<>();

  public static final Registry<Key, AbilityDescription> ABILITIES = create(AbilityDescription.NAMESPACE, AbilityDescription.class);
  public static final Registry<Key, Sequence> SEQUENCES = create(Sequence.NAMESPACE, Sequence.class);
  public static final Registry<Key, CollisionPair> COLLISIONS = create(CollisionPair.NAMESPACE, CollisionPair.class);
  public static final Registry<Key, Protection> PROTECTIONS = create(Protection.NAMESPACE, Protection.class);
  public static final Registry<Key, Translation> TRANSLATIONS = create(Translation.NAMESPACE, Translation.class);
  public static final UserRegistry BENDERS = new UserRegistry();

  private Registries() {
  }

  public static Stream<Key> keys() {
    return REGISTRY_KEYS.stream();
  }

  public static Registry<Key, ?> get(Key type) {
    return REGISTRIES_BY_KEY.get(type);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Keyed> @Nullable Registry<Key, T> get(Class<T> type) {
    return (Registry<Key, T>) REGISTRIES_BY_CLASS.get(type);
  }

  private static <T extends Keyed> Registry<Key, T> create(String namespace, Class<T> clazz) {
    Registry<Key, T> registry = Registry.<T>simpleBuilder(namespace).build();
    REGISTRY_KEYS.add(registry.key());
    REGISTRIES_BY_KEY.put(registry.key(), registry);
    REGISTRIES_BY_CLASS.put(clazz, registry);
    return registry;
  }
}
