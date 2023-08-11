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

package me.moros.bending;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import me.moros.bending.CollisionUtil.CachedAbility;
import me.moros.bending.CollisionUtil.CollectionType;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.common.collision.LBVH;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 50, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 50, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class CollisionBenchmark {
  @Param({"1", "4", "10"}) // size * 30 players * 2 abilities each
  int size;

  CachedAbility[] abilities;

  @Setup
  public void setup() {
    // Generate up to 10 extra colliders per ability to stress the BVH
    abilities = CollisionUtil.generateColliders(size, true);
  }

  @Benchmark
  public void processCollisions(Blackhole bh) {
    Collection<CachedAbility> pruned = CollectionType.IdentityHashSet.create(abilities.length / 2);
    for (var first : abilities) {
      if (pruned.contains(first)) {
        continue;
      }
      for (var second : abilities) {
        if (handleInternal(first, second, pruned, bh)) {
          break;
        }
      }
    }
    bh.consume(pruned);
  }

  @Benchmark
  public void processCollisionsParallel(Blackhole bh) {
    Collection<CachedAbility> pruned = CollectionType.ConcurrentHashSet.create(abilities.length / 2);
    Arrays.stream(abilities).parallel().forEach(first -> {
      if (!pruned.contains(first)) {
        for (var second : abilities) {
          if (handleInternal(first, second, pruned, bh)) {
            break;
          }
        }
      }
    });
    bh.consume(pruned);
  }

  @Benchmark
  public void processCollisionsLBVH(Blackhole bh) {
    var copy = new CachedAbility[abilities.length];
    System.arraycopy(abilities, 0, copy, 0, abilities.length);
    var bvh = LBVH.buildTree(copy);
    Collection<CachedAbility> pruned = CollectionType.IdentityHashSet.create(abilities.length / 2);
    for (var pair : bvh.queryAll()) {
      var first = pair.first();
      if (!pruned.contains(first)) {
        handleInternal(first, pair.second(), pruned, bh);
      }
    }
    bh.consume(pruned);
  }

  private boolean handleInternal(CachedAbility first, CachedAbility second, Collection<CachedAbility> pruned, Blackhole bh) {
    if (first.uuid().equals(second.uuid()) || pruned.contains(second)) {
      return false;
    }
    Entry<Collider, Collider> collision = checkCollision(first.colliders(), second.colliders());
    if (collision != null) {
      bh.consume(collision.getKey());
      bh.consume(collision.getValue());
      Blackhole.consumeCPU(512);
      int idx = pruned.size() % 3;
      if (idx == 0) {
        pruned.add(first);
        return true;
      } else if (idx == 1) {
        pruned.add(second);
      }
    }
    return false;
  }

  private @Nullable Entry<Collider, Collider> checkCollision(Collection<Collider> firstColliders, Collection<Collider> secondColliders) {
    for (Collider firstCollider : firstColliders) {
      for (Collider secondCollider : secondColliders) {
        if (firstCollider.intersects(secondCollider)) {
          return Map.entry(firstCollider, secondCollider);
        }
      }
    }
    return null;
  }
}
