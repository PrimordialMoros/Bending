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

import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.user.User;

final class DummyAbilityManager implements AbilityManager {
  static final AbilityManager INSTANCE = new DummyAbilityManager();

  private DummyAbilityManager() {
  }

  @Override
  public void addAbility(User user, Ability instance) {
  }

  @Override
  public void changeOwner(Ability ability, User user) {
  }

  @Override
  public void createPassives(User user) {
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void destroyInstance(Ability ability) {
  }

  @Override
  public boolean destroyUserInstances(User user, Iterable<Predicate<Ability>> predicates) {
    return true;
  }

  @Override
  public Stream<Ability> userInstances(User user) {
    return Stream.empty();
  }

  @Override
  public Stream<Ability> instances() {
    return Stream.empty();
  }

  @Override
  public void destroyUserInstances(User user) {
  }

  @Override
  public void destroyAllInstances() {
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.REMOVE;
  }
}