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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionPair;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.collision.AABBUtil;
import me.moros.bending.common.collision.Boundable;
import me.moros.bending.common.collision.CollisionData;
import me.moros.bending.common.collision.CollisionQuery;
import me.moros.bending.common.collision.CollisionQuery.Pair;
import me.moros.bending.common.collision.LBVH;
import me.moros.bending.common.collision.MortonEncoded;
import me.moros.math.FastMath;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CollisionManager implements Updatable {
  private final AbilityManager manager;

  CollisionManager(AbilityManager manager) {
    this.manager = manager;
  }

  private CachedAbility[] filterAndCollect() {
    Collection<CachedAbility> instances = new ArrayList<>(FastMath.ceil(0.5 * manager.size()));
    for (Ability ability : manager) {
      Collection<Collider> colliders = ability.colliders();
      if (!colliders.isEmpty()) {
        instances.add(CachedAbility.create(ability, colliders));
      }
    }
    return instances.toArray(CachedAbility[]::new);
  }

  @Override
  public UpdateResult update() {
    CachedAbility[] instances = filterAndCollect();
    if (instances.length < 2) {
      return UpdateResult.CONTINUE;
    }
    Set<CachedAbility> pruned = Collections.newSetFromMap(new IdentityHashMap<>(instances.length));
    LBVH<CachedAbility> bvh = LBVH.buildTree(instances);
    CollisionQuery<CachedAbility> query = bvh.queryAll();
    for (Pair<CachedAbility> pair : query) {
      processPotentialCollision(pair, pruned);
    }
    return UpdateResult.CONTINUE;
  }

  private void processPotentialCollision(Pair<CachedAbility> queryPair, Set<CachedAbility> pruned) {
    CachedAbility firstEntry = queryPair.first();
    CachedAbility secondEntry = queryPair.second();
    if (firstEntry.isSameUser(secondEntry) || pruned.contains(firstEntry) || pruned.contains(secondEntry)) {
      return;
    }
    Ability first = firstEntry.ability();
    Ability second = secondEntry.ability();
    CollisionPair pair = Registries.COLLISIONS.get(CollisionPair.createKey(first.description(), second.description()));
    if (pair == null) {
      return;
    }
    Entry<Collider, Collider> collision = checkCollision(firstEntry.colliders(), secondEntry.colliders());
    if (collision != null) {
      CollisionData result = handleCollision(first, second, collision.getKey(), collision.getValue(), pair);
      if (result.removeFirst()) {
        manager.destroyInstance(first);
        pruned.add(firstEntry);
      }
      if (result.removeSecond()) {
        manager.destroyInstance(second);
        pruned.add(secondEntry);
      }
    }
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
    boolean invertOrder = first.description() != pair.first();
    boolean removeFirst = invertOrder ? pair.removeSecond() : pair.removeFirst();
    boolean removeSecond = invertOrder ? pair.removeFirst() : pair.removeSecond();
    CollisionData data = new CollisionData(first, second, c1, c2, removeFirst, removeSecond);
    first.onCollision(data.asCollision());
    second.onCollision(data.asInverseCollision());
    return data;
  }

  private record CachedAbility(Ability ability, Collection<Collider> colliders, AABB box,
                               int morton) implements Boundable, MortonEncoded {
    private boolean isSameUser(CachedAbility other) {
      return ability.user().uuid().equals(other.ability.user().uuid());
    }

    private static CachedAbility create(Ability ability, Collection<Collider> colliders) {
      AABB box = AABBUtil.combine(colliders);
      return new CachedAbility(ability, colliders, box, MortonEncoded.calculateMorton(box.position()));
    }
  }
}
