/*
 * Copyright 2020-2024 Moros
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
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
  private final Map<UUID, Queue<Ability>> globalInstances;

  private final Collection<Updatable> pending;
  private final MultiUpdatable<Updatable> generics;

  private int size;

  AbilityManagerImpl(Logger logger, Key world) {
    this.logger = logger;
    this.world = world;
    globalInstances = new ConcurrentHashMap<>(32);
    pending = new ArrayList<>();
    generics = MultiUpdatable.empty();
  }

  private void addAbilityInternal(UUID uuid, Ability instance) {
    globalInstances.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>()).add(instance);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Iterator<Ability> iterator() {
    return instances().iterator();
  }

  @Override
  public void addUpdatable(Updatable instance) {
    if (instance instanceof Ability ability) {
      addAbility(ability);
    } else {
      pending.add(instance);
    }
  }

  @Override
  public void addAbility(Ability instance) {
    User user = instance.user();
    if (world.equals(user.worldKey())) {
      addAbilityInternal(user.uuid(), instance);
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
          addAbility(ability);
        }
      }
    }
  }

  @Override
  public void changeOwner(Ability ability, User user) {
    if (ability.user().equals(user) || !ability.user().worldKey().equals(user.worldKey()) || !world.equals(user.worldKey())) {
      return;
    }
    Collection<Ability> holder = globalInstances.get(ability.user().uuid());
    if (holder != null && holder.remove(ability)) {
      ability.onUserChange(user);
      ability.loadConfig();
      addAbilityInternal(user.uuid(), ability);
    }
  }

  @Override
  public Stream<Ability> userInstances(User user) {
    Collection<Ability> holder = globalInstances.get(user.uuid());
    return holder != null ? holder.stream() : Stream.of();
  }

  @Override
  public Stream<Ability> instances() {
    return globalInstances.values().stream().flatMap(Collection::stream);
  }

  @Override
  public UpdateResult update() {
    // Update all instances and remove invalid instances
    pending.forEach(generics::add);
    pending.clear();
    generics.update();

    Collection<Exception> exceptions = new ArrayList<>();
    var iterator = globalInstances.values().iterator();
    size = 0;
    while (iterator.hasNext()) {
      Collection<Ability> abilities = iterator.next();
      Iterator<Ability> innerIterator = abilities.iterator();
      while (innerIterator.hasNext()) {
        Ability ability = innerIterator.next();
        UpdateResult result = UpdateResult.REMOVE;
        try {
          result = ability.update();
        } catch (Exception e) {
          exceptions.add(e);
        } finally {
          if (result == UpdateResult.REMOVE) {
            innerIterator.remove();
            ability.onDestroy();
          } else {
            size++;
          }
        }
      }
      if (abilities.isEmpty()) {
        iterator.remove();
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
    Collection<Ability> holder = globalInstances.get(user.uuid());
    if (holder != null) {
      Iterator<Ability> iterator = holder.iterator();
      while (iterator.hasNext()) {
        Ability ability = iterator.next();
        if (predicate.test(ability)) {
          iterator.remove();
          ability.onDestroy();
          destroyed = true;
        }
      }
    }
    return destroyed;
  }

  @Override
  public void destroyUserInstances(User user) {
    Collection<Ability> holder = globalInstances.remove(user.uuid());
    if (holder != null) {
      holder.forEach(Ability::onDestroy);
      holder.clear();
    }
  }

  @Override
  public void destroyInstance(Ability ability) {
    Collection<Ability> holder = globalInstances.get(ability.user().uuid());
    if (holder != null && holder.remove(ability)) {
      ability.onDestroy();
    }
  }

  @Override
  public void destroyAllInstances() {
    pending.clear();
    generics.clear();
    for (Collection<Ability> holder : globalInstances.values()) {
      holder.forEach(Ability::onDestroy);
      holder.clear();
    }
    globalInstances.clear();
  }
}
