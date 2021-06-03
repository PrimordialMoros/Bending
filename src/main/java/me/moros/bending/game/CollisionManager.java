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

package me.moros.bending.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.RegisteredCollision;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;

// TODO implement BVH and profile
public final class CollisionManager {
  private final AbilityManager manager;

  CollisionManager(@NonNull AbilityManager manager) {
    this.manager = manager;
  }

  public void update() {
    Collection<Ability> instances = manager.instances().filter(ability -> !ability.colliders().isEmpty())
      .collect(Collectors.toList());
    if (instances.size() < 2) {
      return;
    }
    Map<Ability, Collection<Collider>> colliderCache = new HashMap<>(instances.size());
    Map<AbilityDescription, Collection<Ability>> abilityCache = new HashMap<>(32);
    for (RegisteredCollision registeredCollision : Registries.COLLISIONS) {
      Collection<Ability> firstAbilities = abilityCache.computeIfAbsent(registeredCollision.first(), desc ->
        instances.stream().filter(ability -> ability.description().equals(desc)).collect(Collectors.toList())
      );
      Collection<Ability> secondAbilities = abilityCache.computeIfAbsent(registeredCollision.second(), desc ->
        instances.stream().filter(ability -> ability.description().equals(desc)).collect(Collectors.toList())
      );
      for (Ability first : firstAbilities) {
        Collection<Collider> firstColliders = colliderCache.computeIfAbsent(first, Ability::colliders);
        if (firstColliders.isEmpty()) {
          continue;
        }
        for (Ability second : secondAbilities) {
          if (first.user().equals(second.user())) {
            continue;
          }
          Collection<Collider> secondColliders = colliderCache.computeIfAbsent(second, Ability::colliders);
          if (secondColliders.isEmpty()) {
            continue;
          }
          Map.Entry<Collider, Collider> collisionResult = checkCollision(firstColliders, secondColliders);
          if (collisionResult != null) {
            handleCollision(first, second, collisionResult.getKey(), collisionResult.getValue(), registeredCollision);
          }
        }
      }
    }
  }

  private Map.Entry<Collider, Collider> checkCollision(Collection<Collider> firstColliders, Collection<Collider> secondColliders) {
    for (Collider firstCollider : firstColliders) {
      for (Collider secondCollider : secondColliders) {
        if (firstCollider.intersects(secondCollider)) {
          return Map.entry(firstCollider, secondCollider);
        }
      }
    }
    return null;
  }

  private void handleCollision(Ability first, Ability second, Collider c1, Collider c2, RegisteredCollision rc) {
    Collision.CollisionData data = new Collision.CollisionData(first, second, c1, c2, rc.removeFirst(), rc.removeSecond());
    first.onCollision(data.asCollision());
    second.onCollision(data.asInverseCollision());
    if (data.removeFirst()) {
      manager.destroyInstance(first);
    }
    if (data.removeSecond()) {
      manager.destroyInstance(second);
    }
  }
}
