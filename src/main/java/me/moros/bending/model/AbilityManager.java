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

package me.moros.bending.model;

import java.util.Optional;
import java.util.stream.Stream;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface AbilityManager {
  default void addAbility(@NonNull User user, @NonNull Ability instance) {
  }

  default void changeOwner(@NonNull Ability ability, @NonNull User user) {
  }

  default void createPassives(@NonNull User user) {
  }

  default int size() {
    return 0;
  }

  default <T extends Ability> boolean hasAbility(@NonNull User user, @NonNull Class<T> type) {
    return false;
  }

  default void destroyInstance(@NonNull Ability ability) {
  }

  default <T extends Ability> boolean destroyInstanceType(@NonNull User user, @NonNull Class<T> type) {
    return true;
  }

  default @NonNull Stream<Ability> userInstances(@NonNull User user) {
    return Stream.empty();
  }

  default <T extends Ability> @NonNull Stream<T> userInstances(@NonNull User user, @NonNull Class<T> type) {
    return Stream.empty();
  }

  default <T extends Ability> Optional<T> firstInstance(@NonNull User user, @NonNull Class<T> type) {
    return Optional.empty();
  }

  default @NonNull Stream<Ability> instances() {
    return Stream.empty();
  }

  default <T extends Ability> @NonNull Stream<T> instances(@NonNull Class<T> type) {
    return Stream.empty();
  }

  default void destroyUserInstances(@NonNull User user) {
  }

  default void destroyAllInstances() {
  }

  default void update() {
  }
}
