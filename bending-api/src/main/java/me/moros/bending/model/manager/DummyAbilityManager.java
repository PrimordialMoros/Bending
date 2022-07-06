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
import java.util.stream.Stream;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DummyAbilityManager implements AbilityManager {
  public static final AbilityManager DUMMY = new DummyAbilityManager();

  private DummyAbilityManager() {
  }

  @Override
  public void addAbility(@NonNull User user, @NonNull Ability instance) {
  }

  @Override
  public void changeOwner(@NonNull Ability ability, @NonNull User user) {
  }

  @Override
  public void createPassives(@NonNull User user) {
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void destroyInstance(@NonNull Ability ability) {
  }

  @Override
  public boolean destroyInstanceType(@NonNull User user, @NonNull Collection<@NonNull Class<? extends Ability>> types) {
    return true;
  }

  @Override
  public @NonNull Stream<Ability> userInstances(@NonNull User user) {
    return Stream.empty();
  }

  @Override
  public @NonNull Stream<Ability> instances() {
    return Stream.empty();
  }

  @Override
  public void destroyUserInstances(@NonNull User user) {
  }

  @Override
  public void destroyAllInstances() {
  }

  @Override
  public @NonNull UpdateResult update() {
    return UpdateResult.REMOVE;
  }
}
