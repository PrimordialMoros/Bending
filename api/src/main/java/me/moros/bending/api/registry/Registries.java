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

package me.moros.bending.api.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityDescription.Sequence;
import me.moros.bending.api.collision.CollisionPair;
import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.protection.Protection;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jspecify.annotations.Nullable;

/**
 * Holds all the built-in registries.
 */
@SuppressWarnings("unchecked")
public final class Registries {
  private static final Map<DataKey<?>, Registry<Key, ? extends Keyed>> REGISTRIES_BY_KEY = new HashMap<>();

  public static final Registry<Key, AbilityDescription> ABILITIES = create("ability", AbilityDescription.class);
  public static final Registry<Key, Sequence> SEQUENCES = create("sequence", Sequence.class);
  public static final Registry<Key, CollisionPair> COLLISIONS = create("collision", CollisionPair.class);
  public static final Registry<Key, Protection> PROTECTIONS = create("protection", Protection.class);
  public static final Registry<Key, Translation> TRANSLATIONS = create("translation", Translation.class);
  public static final UserRegistry BENDERS = new UserRegistry();

  private Registries() {
  }

  public static Stream<DataKey<?>> keys() {
    return REGISTRIES_BY_KEY.keySet().stream();
  }

  public static <T> @Nullable Registry<Key, T> get(DataKey<T> type) {
    return (Registry<Key, T>) REGISTRIES_BY_KEY.get(type);
  }

  public static <T> Registry<Key, T> getOrThrow(DataKey<T> type) {
    return Objects.requireNonNull(get(type));
  }

  private static <T extends Keyed> Registry<Key, T> create(String name, Class<T> clazz) {
    Registry<Key, T> registry = Registry.simpleBuilder(KeyUtil.data("registry." + name, clazz)).build();
    REGISTRIES_BY_KEY.put(registry.key(), registry);
    return registry;
  }
}
