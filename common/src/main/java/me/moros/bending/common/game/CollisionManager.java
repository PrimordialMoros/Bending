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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.Collision.CollisionData;
import me.moros.bending.api.collision.CollisionPair;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.registry.Registries;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CollisionManager implements Updatable {
  private final AbilityManager manager;

  CollisionManager(AbilityManager manager) {
    this.manager = manager;
  }

  @Override
  public UpdateResult update() {
    CachedAbility[] instances = filterAndCollect();
    if (instances.length < 2) {
      return UpdateResult.CONTINUE;
    }
    Collection<CachedAbility> pruned = Collections.newSetFromMap(new IdentityHashMap<>(instances.length));
    for (CachedAbility firstEntry : instances) {
      if (pruned.contains(firstEntry)) {
        continue;
      }
      Ability first = firstEntry.ability();
      for (var secondEntry : instances) {
        Ability second = secondEntry.ability();
        if (first.user().equals(second.user()) || pruned.contains(secondEntry)) {
          continue;
        }
        CollisionPair pair = Registries.COLLISIONS.get(CollisionPair.createKey(first.description(), second.description()));
        if (pair != null) {
          Entry<Collider, Collider> collision = checkCollision(firstEntry.colliders(), secondEntry.colliders());
          if (collision != null) {
            CollisionData result = handleCollision(first, second, collision.getKey(), collision.getValue(), pair);
            if (result.removeFirst()) {
              manager.destroyInstance(first);
              pruned.add(firstEntry);
              break;
            }
            if (result.removeSecond()) {
              manager.destroyInstance(second);
              pruned.add(secondEntry);
            }
          }
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  private CachedAbility[] filterAndCollect() {
    Collection<CachedAbility> instances = new ArrayList<>(manager.size());
    for (Ability ability : manager) {
      Collection<Collider> colliders = ability.colliders();
      if (!colliders.isEmpty()) {
        instances.add(new CachedAbility(ability, colliders));
      }
    }
    return instances.toArray(CachedAbility[]::new);
  }

  private @Nullable Entry<Collider, Collider> checkCollision(Iterable<Collider> firstColliders, Iterable<Collider> secondColliders) {
    for (Collider firstCollider : firstColliders) {
      for (Collider secondCollider : secondColliders) {
        if (firstCollider.intersects(secondCollider)) {
          return Map.entry(firstCollider, secondCollider);
        }
      }
    }
    return null;
  }

  private CollisionData handleCollision(Ability first, Ability second, Collider c1, Collider c2, CollisionPair pair) {
    CollisionData data = new CollisionData(first, second, c1, c2, pair.removeFirst(), pair.removeSecond());
    first.onCollision(data.asCollision());
    second.onCollision(data.asInverseCollision());
    return data;
  }

  private record CachedAbility(Ability ability, Collection<Collider> colliders) {
  }
}
