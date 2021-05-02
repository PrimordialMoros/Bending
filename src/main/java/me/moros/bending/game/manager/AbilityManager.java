/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AbilityManager {
  private final Multimap<User, Ability> globalInstances;
  private final Collection<UserInstance> addQueue;

  @SuppressWarnings("UnstableApiUsage")
  protected AbilityManager() {
    globalInstances = MultimapBuilder.hashKeys(32).arrayListValues(16).build();
    addQueue = new ArrayList<>(32);
  }

  private static class UserInstance {
    private final User user;
    private final Ability ability;

    private UserInstance(User user, Ability ability) {
      this.user = user;
      this.ability = ability;
    }
  }

  public void addAbility(@NonNull User user, @NonNull Ability instance) {
    addQueue.add(new UserInstance(user, instance));
  }

  public void changeOwner(@NonNull Ability ability, @NonNull User user) {
    if (ability.user().equals(user) || !ability.user().world().equals(user.world())) {
      return;
    }
    if (ability.user(user) && globalInstances.remove(ability.user(), ability)) {
      ability.recalculateConfig();
      globalInstances.put(user, ability);
    }
  }

  public void createPassives(@NonNull User user) {
    Collection<AbilityDescription> userPassives = user.elements().stream()
      .flatMap(Bending.game().abilityRegistry()::passives).collect(Collectors.toList());
    for (AbilityDescription passive : userPassives) {
      destroyInstanceType(user, passive);
      if (user.hasPermission(passive)) {
        Ability ability = passive.createAbility();
        if (ability.activate(user, ActivationMethod.PASSIVE)) {
          addAbility(user, ability);
        }
      }
    }
  }

  public void clearPassives(@NonNull User user) {
    userInstances(user).filter(a -> a.description().isActivatedBy(ActivationMethod.PASSIVE)).forEach(this::destroyAbility);
  }

  public int size() {
    return globalInstances.size();
  }

  public <T extends Ability> boolean hasAbility(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user, type).findAny().isPresent();
  }

  public boolean hasAbility(@NonNull User user, @NonNull AbilityDescription desc) {
    return hasAbility(user, desc.createAbility().getClass());
  }

  public void destroyInstance(@NonNull Ability ability) {
    if (globalInstances.remove(ability.user(), ability)) {
      destroyAbility(ability);
    }
  }

  public boolean destroyInstanceType(@NonNull User user, @NonNull AbilityDescription desc) {
    return destroyInstanceType(user, desc.createAbility().getClass());
  }

  public <T extends Ability> boolean destroyInstanceType(@NonNull User user, @NonNull Class<T> type) {
    boolean destroyed = false;
    Iterator<Ability> iterator = globalInstances.get(user).iterator();
    while (iterator.hasNext()) {
      Ability ability = iterator.next();
      if (type.isInstance(ability)) {
        iterator.remove();
        destroyAbility(ability);
        destroyed = true;
      }
    }
    return destroyed;
  }

  public @NonNull Stream<Ability> userInstances(@NonNull User user) {
    return globalInstances.get(user).stream();
  }

  public <T extends Ability> @NonNull Stream<T> userInstances(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user).filter(type::isInstance).map(type::cast);
  }

  public <T extends Ability> Optional<T> firstInstance(@NonNull User user, @NonNull Class<T> type) {
    return userInstances(user, type).findFirst();
  }

  public @NonNull Stream<Ability> instances() {
    return globalInstances.values().stream();
  }

  public <T extends Ability> @NonNull Stream<T> instances(@NonNull Class<T> type) {
    return instances().filter(type::isInstance).map(type::cast);
  }

  public void destroyUserInstances(@NonNull User user) {
    globalInstances.removeAll(user).forEach(this::destroyAbility);
  }

  public void destroyAllInstances() {
    globalInstances.values().forEach(this::destroyAbility);
    globalInstances.clear();
  }

  // Updates each ability every tick. Destroys the ability if ability.update() returns UpdateResult.Remove.
  public void update() {
    for (UserInstance i : addQueue) {
      globalInstances.put(i.user, i.ability);
    }
    addQueue.clear();
    Iterator<Ability> globalIterator = globalInstances.values().iterator();
    while (globalIterator.hasNext()) {
      Ability ability = globalIterator.next();
      UpdateResult result = UpdateResult.REMOVE;
      try (MCTiming timing = Bending.timingManager().of(ability.description().name()).startTiming()) {
        result = ability.update();
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (result == UpdateResult.REMOVE) {
        globalIterator.remove();
        destroyAbility(ability);
      }
    }
  }

  private void destroyAbility(@NonNull Ability ability) {
    ability.onDestroy();
  }
}
