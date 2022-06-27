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

package me.moros.bending.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.moros.bending.Bending;
import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Updatable.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AbilityManagerImpl implements AbilityManager {
  private final Multimap<UUID, Ability> globalInstances;
  private final Collection<Entry<UUID, Ability>> addQueue;

  AbilityManagerImpl() {
    globalInstances = MultimapBuilder.hashKeys(32).arrayListValues(16).build();
    addQueue = new ArrayList<>(16);
  }

  @Override
  public void addAbility(@NonNull User user, @NonNull Ability instance) {
    addQueue.add(Map.entry(user.uuid(), instance));
  }

  @Override
  public void changeOwner(@NonNull Ability ability, @NonNull User user) {
    if (ability.user().equals(user) || !ability.user().world().equals(user.world())) {
      return;
    }
    if (globalInstances.remove(ability.user().uuid(), ability)) {
      ability.onUserChange(user);
      ability.loadConfig();
      globalInstances.put(user.uuid(), ability);
    }
  }

  @Override
  public void createPassives(@NonNull User user) {
    Collection<AbilityDescription> allPassives = Registries.ABILITIES.stream()
      .filter(d -> d.isActivatedBy(Activation.PASSIVE)).toList();
    for (AbilityDescription passive : allPassives) {
      destroyInstanceType(user, passive.createAbility().getClass());
      if (user.hasElement(passive.element()) && user.hasPermission(passive)) {
        Ability ability = passive.createAbility();
        if (ability.activate(user, Activation.PASSIVE)) {
          addAbility(user, ability);
        }
      }
    }
  }

  @Override
  public int size() {
    return globalInstances.size();
  }

  @Override
  public <T extends Ability> boolean hasAbility(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user, type).findAny().isPresent();
  }

  @Override
  public void destroyInstance(@NonNull Ability ability) {
    if (globalInstances.remove(ability.user().uuid(), ability)) {
      ability.onDestroy();
    }
  }

  @Override
  public boolean destroyInstanceType(@NonNull User user, @NonNull Collection<Class<? extends Ability>> types) {
    boolean destroyed = false;
    Iterator<Ability> iterator = globalInstances.get(user.uuid()).iterator();
    while (iterator.hasNext()) {
      Ability ability = iterator.next();
      for (var type : types) {
        if (type.isInstance(ability)) {
          iterator.remove();
          ability.onDestroy();
          destroyed = true;
        }
      }
    }
    return destroyed;
  }

  @Override
  public @NonNull Stream<Ability> userInstances(@NonNull User user) {
    return globalInstances.get(user.uuid()).stream();
  }

  @Override
  public <T extends Ability> @NonNull Stream<T> userInstances(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user).filter(type::isInstance).map(type::cast);
  }

  @Override
  public <T extends Ability> Optional<T> firstInstance(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user, type).findFirst();
  }

  @Override
  public @NonNull Stream<Ability> instances() {
    return globalInstances.values().stream();
  }

  @Override
  public <T extends Ability> @NonNull Stream<T> instances(@NonNull Class<T> type) {
    return instances().filter(type::isInstance).map(type::cast);
  }

  @Override
  public void destroyUserInstances(@NonNull User user) {
    globalInstances.removeAll(user.uuid()).forEach(Ability::onDestroy);
  }

  @Override
  public void destroyAllInstances() {
    globalInstances.values().forEach(Ability::onDestroy);
    globalInstances.clear();
  }

  @Override
  public void update() {
    // Add any queued abilities to global instances
    addQueue.forEach(entry -> globalInstances.put(entry.getKey(), entry.getValue()));
    addQueue.clear();
    // Update all instances and remove invalid instances
    Iterator<Ability> globalIterator = globalInstances.values().iterator();
    while (globalIterator.hasNext()) {
      Ability ability = globalIterator.next();
      UpdateResult result = UpdateResult.REMOVE;
      try (Timing timing = Timings.of(Bending.plugin(), ability.description().name()).startTiming()) {
        result = ability.update();
      } catch (Exception e) {
        Bending.logger().warn(e.getMessage(), e);
      }
      if (result == UpdateResult.REMOVE) {
        globalIterator.remove();
        ability.onDestroy();
      }
    }
  }
}
