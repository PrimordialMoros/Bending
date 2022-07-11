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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.slf4j.Logger;

public class AbilityManagerImpl implements AbilityManager {
  private final Logger logger;
  private final Multimap<UUID, Ability> globalInstances;
  private final Collection<Entry<UUID, Ability>> addQueue;

  AbilityManagerImpl(Logger logger) {
    this.logger = logger;
    globalInstances = MultimapBuilder.hashKeys(32).arrayListValues(16).build();
    addQueue = new ArrayList<>(16);
  }

  @Override
  public void addAbility(User user, Ability instance) {
    addQueue.add(Map.entry(user.uuid(), instance));
  }

  @Override
  public void changeOwner(Ability ability, User user) {
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
  public void createPassives(User user) {
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
  public void destroyInstance(Ability ability) {
    if (globalInstances.remove(ability.user().uuid(), ability)) {
      ability.onDestroy();
    }
  }

  @Override
  public boolean destroyInstanceType(User user, Collection<Class<? extends Ability>> types) {
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
  public Stream<Ability> userInstances(User user) {
    return globalInstances.get(user.uuid()).stream();
  }

  @Override
  public Stream<Ability> instances() {
    return globalInstances.values().stream();
  }

  @Override
  public void destroyUserInstances(User user) {
    globalInstances.removeAll(user.uuid()).forEach(Ability::onDestroy);
  }

  @Override
  public void destroyAllInstances() {
    globalInstances.values().forEach(Ability::onDestroy);
    globalInstances.clear();
  }

  @Override
  public UpdateResult update() {
    // Add any queued abilities to global instances
    addQueue.forEach(entry -> globalInstances.put(entry.getKey(), entry.getValue()));
    addQueue.clear();
    // Update all instances and remove invalid instances
    Iterator<Ability> globalIterator = globalInstances.values().iterator();
    Collection<Exception> exceptions = new LinkedList<>();
    while (globalIterator.hasNext()) {
      Ability ability = globalIterator.next();
      UpdateResult result = UpdateResult.REMOVE;
      try {
        result = ability.update();
      } catch (Exception e) {
        exceptions.add(e);
      }
      if (result == UpdateResult.REMOVE) {
        globalIterator.remove();
        ability.onDestroy();
      }
    }
    for (Exception e : exceptions) {
      logger.error(e.getMessage(), e);
    }
    return UpdateResult.CONTINUE;
  }
}
