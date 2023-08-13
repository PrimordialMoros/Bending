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

package me.moros.bending.common.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.logging.Logger;
import net.kyori.adventure.key.Key;

public class AbilityManagerImpl implements AbilityManager {
  private final Logger logger;
  private final Key world;
  private final Multimap<UUID, Ability> globalInstances;
  private final Collection<Entry<UUID, Ability>> addQueue;
  private final MultiUpdatable<Updatable> generics;

  AbilityManagerImpl(Logger logger, Key world) {
    this.logger = logger;
    this.world = world;
    globalInstances = Multimaps.newMultimap(new ConcurrentHashMap<>(32), () -> new ArrayList<>(8));
    addQueue = new ArrayList<>(16);
    generics = MultiUpdatable.empty();
  }

  @Override
  public int size() {
    return globalInstances.size();
  }

  @Override
  public Iterator<Ability> iterator() {
    return Collections.unmodifiableCollection(globalInstances.values()).iterator();
  }

  @Override
  public void addUpdatable(Updatable instance) {
    if (instance instanceof Ability ability) {
      addAbility(ability.user(), ability);
    } else {
      generics.add(instance);
    }
  }

  @Override
  public void addAbility(User user, Ability instance) {
    if (world.equals(user.worldKey())) {
      addQueue.add(Map.entry(user.uuid(), instance));
    }
  }

  @Override
  public void createPassives(User user) {
    if (!world.equals(user.worldKey())) {
      return;
    }
    Predicate<AbilityDescription> isPassive = desc -> desc.isActivatedBy(Activation.PASSIVE);
    Collection<AbilityDescription> allPassives = Registries.ABILITIES.stream().filter(isPassive).toList();
    destroyUserInstances(user, a -> isPassive.test(a.description()));
    for (AbilityDescription passive : allPassives) {
      if (user.hasElement(passive.element()) && user.hasPermission(passive)) {
        Ability ability = passive.createAbility();
        if (ability.activate(user, Activation.PASSIVE)) {
          addAbility(user, ability);
        }
      }
    }
  }

  @Override
  public void changeOwner(Ability ability, User user) {
    if (ability.user().equals(user) || !ability.user().worldKey().equals(user.worldKey()) || !world.equals(user.worldKey())) {
      return;
    }
    if (globalInstances.remove(ability.user().uuid(), ability)) {
      ability.onUserChange(user);
      ability.loadConfig();
      globalInstances.put(user.uuid(), ability);
    }
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
  public UpdateResult update() {
    // Add any queued abilities to global instances
    addQueue.forEach(entry -> globalInstances.put(entry.getKey(), entry.getValue()));
    addQueue.clear();
    // Update all instances and remove invalid instances
    generics.update();
    Collection<Ability> copy = new ArrayList<>(globalInstances.values());
    Iterator<Ability> copyIterator = copy.iterator();
    Collection<Exception> exceptions = new ArrayList<>();
    while (copyIterator.hasNext()) {
      Ability ability = copyIterator.next();
      UUID uuid = ability.user().uuid();
      UpdateResult result = UpdateResult.REMOVE;
      try {
        result = ability.update();
      } catch (Exception e) {
        exceptions.add(e);
      } finally {
        if (result == UpdateResult.REMOVE) {
          copyIterator.remove();
          globalInstances.remove(uuid, ability);
          ability.onDestroy();
        }
      }
    }
    for (Exception e : exceptions) {
      logger.error(e.getMessage(), e);
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public boolean destroyUserInstances(User user, Predicate<Ability> predicate) {
    boolean destroyed = false;
    Iterator<Ability> iterator = globalInstances.get(user.uuid()).iterator();
    while (iterator.hasNext()) {
      Ability ability = iterator.next();
      if (predicate.test(ability)) {
        iterator.remove();
        ability.onDestroy();
        destroyed = true;
      }
    }
    return destroyed;
  }

  @Override
  public void destroyUserInstances(User user) {
    globalInstances.removeAll(user.uuid()).forEach(Ability::onDestroy);
  }

  @Override
  public void destroyInstance(Ability ability) {
    if (globalInstances.remove(ability.user().uuid(), ability)) {
      ability.onDestroy();
    }
  }

  @Override
  public void destroyAllInstances() {
    generics.clear();
    globalInstances.values().forEach(Ability::onDestroy);
    globalInstances.clear();
  }
}
