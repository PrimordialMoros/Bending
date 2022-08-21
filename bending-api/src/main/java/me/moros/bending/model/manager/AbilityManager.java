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

package me.moros.bending.model.manager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.user.User;

/**
 * Handles active {@link Ability} instances.
 */
public interface AbilityManager extends Updatable {
  void addAbility(User user, Ability instance);

  void changeOwner(Ability ability, User user);

  void createPassives(User user);

  int size();

  default <T extends Ability> boolean hasAbility(User user, Class<T> type) {
    return userInstances(user, type).findAny().isPresent();
  }

  void destroyInstance(Ability ability);

  private Predicate<Ability> isInstance(Class<? extends Ability> type) {
    return type::isInstance;
  }

  default boolean destroyUserInstance(User user, Class<? extends Ability> type) {
    return destroyUserInstances(user, isInstance(type));
  }

  default boolean destroyUserInstances(User user, Collection<Class<? extends Ability>> types) {
    var predicates = types.stream().map(this::isInstance).toList();
    return destroyUserInstances(user, predicates);
  }

  default boolean destroyUserInstances(User user, Predicate<Ability> predicate) {
    return destroyUserInstances(user, List.of(predicate));
  }

  boolean destroyUserInstances(User user, Iterable<Predicate<Ability>> predicates);

  Stream<Ability> userInstances(User user);

  default <T extends Ability> Stream<T> userInstances(User user, Class<T> type) {
    return userInstances(user).filter(type::isInstance).map(type::cast);
  }

  default <T extends Ability> Optional<T> firstInstance(User user, Class<T> type) {
    return userInstances(user, type).findFirst();
  }

  Stream<Ability> instances();

  default <T extends Ability> Stream<T> instances(Class<T> type) {
    return instances().filter(type::isInstance).map(type::cast);
  }

  void destroyUserInstances(User user);

  void destroyAllInstances();

  static AbilityManager dummy() {
    return DummyAbilityManager.INSTANCE;
  }
}
